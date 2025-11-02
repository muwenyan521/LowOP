package wangxianming.lowop.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import wangxianming.lowop.LowOP;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class AuditManager {

    private final LowOP plugin;
    private final List<String> auditLog;
    private FileConfiguration auditConfig;
    private File auditFile;
    private final SimpleDateFormat dateFormat;
    private final int maxLogEntries;

    public AuditManager(LowOP plugin) {
        this.plugin = plugin;
        this.auditLog = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        this.maxLogEntries = 1000; // Keep last 1000 entries in memory
        
        loadAuditLog();
        startAutoFlushTask();
    }

    private void loadAuditLog() {
        auditFile = new File(plugin.getDataFolder(), "audit_log.yml");
        if (!auditFile.exists()) {
            try {
                auditFile.getParentFile().mkdirs();
                auditFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create audit_log.yml", e);
                return;
            }
        }
        
        auditConfig = YamlConfiguration.loadConfiguration(auditFile);
        
        // Load recent audit entries
        if (auditConfig.contains("entries")) {
            List<String> entries = auditConfig.getStringList("entries");
            // Keep only the most recent entries up to maxLogEntries
            int startIndex = Math.max(0, entries.size() - maxLogEntries);
            auditLog.addAll(entries.subList(startIndex, entries.size()));
        }
        
        plugin.getLogger().info("Loaded " + auditLog.size() + " audit log entries");
    }

    public void logStateChange(UUID playerUUID, boolean fromState, boolean toState, String executor) {
        String playerName = getPlayerName(playerUUID);
        String timestamp = dateFormat.format(new Date());
        
        String logEntry = String.format("[%s] %s changed %s's admin state from %s to %s",
            timestamp, executor, playerName, fromState ? "ADMIN" : "DEFAULT", toState ? "ADMIN" : "DEFAULT");
        
        addLogEntry(logEntry);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Audit: " + logEntry);
        }
    }

    public void logCommandExecution(String command, String executor, boolean success) {
        String timestamp = dateFormat.format(new Date());
        
        String logEntry = String.format("[%s] %s executed command: %s (Success: %s)",
            timestamp, executor, command, success);
        
        addLogEntry(logEntry);
    }

    public void logError(String operation, String executor, String error) {
        String timestamp = dateFormat.format(new Date());
        
        String logEntry = String.format("[%s] ERROR - %s by %s: %s",
            timestamp, operation, executor, error);
        
        addLogEntry(logEntry);
        plugin.getLogger().warning("Audit Error: " + logEntry);
    }

    public void logBatchOperation(String operation, String executor, int total, int success) {
        String timestamp = dateFormat.format(new Date());
        
        String logEntry = String.format("[%s] %s performed batch operation: %s - %d/%d successful",
            timestamp, executor, operation, success, total);
        
        addLogEntry(logEntry);
    }

    private void addLogEntry(String logEntry) {
        auditLog.add(logEntry);
        
        // Trim log if it exceeds maximum size
        if (auditLog.size() > maxLogEntries) {
            auditLog.subList(0, auditLog.size() - maxLogEntries).clear();
        }
        
        // Auto-save if enabled
        if (plugin.getConfigManager().getAutoSaveInterval() > 0) {
            flushLogs();
        }
    }

    public void flushLogs() {
        if (!plugin.getConfigManager().isAuditLogEnabled()) {
            return;
        }
        
        try {
            auditConfig.set("entries", auditLog);
            auditConfig.save(auditFile);
            plugin.getLogger().fine("Audit log flushed successfully");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save audit log to " + auditFile, e);
        }
    }

    private void startAutoFlushTask() {
        int interval = plugin.getConfigManager().getAutoSaveInterval();
        if (interval > 0) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, this::flushLogs, 
                interval * 20L, interval * 20L); // Convert seconds to ticks
        }
    }

    private String getPlayerName(UUID playerUUID) {
        // Try online player first
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }
        
        // Try offline player
        String name = Bukkit.getOfflinePlayer(playerUUID).getName();
        return name != null ? name : playerUUID.toString();
    }

    // Query methods for audit log
    public List<String> getRecentEntries(int count) {
        int startIndex = Math.max(0, auditLog.size() - count);
        return new ArrayList<>(auditLog.subList(startIndex, auditLog.size()));
    }

    public List<String> getEntriesByExecutor(String executor, int maxResults) {
        List<String> result = new ArrayList<>();
        for (int i = auditLog.size() - 1; i >= 0 && result.size() < maxResults; i--) {
            String entry = auditLog.get(i);
            if (entry.contains(" " + executor + " ")) {
                result.add(entry);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public List<String> getEntriesByPlayer(String playerName, int maxResults) {
        List<String> result = new ArrayList<>();
        for (int i = auditLog.size() - 1; i >= 0 && result.size() < maxResults; i--) {
            String entry = auditLog.get(i);
            if (entry.contains(" " + playerName + "'s")) {
                result.add(entry);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public List<String> searchEntries(String keyword, int maxResults) {
        List<String> result = new ArrayList<>();
        for (int i = auditLog.size() - 1; i >= 0 && result.size() < maxResults; i--) {
            String entry = auditLog.get(i);
            if (entry.toLowerCase().contains(keyword.toLowerCase())) {
                result.add(entry);
            }
        }
        Collections.reverse(result);
        return result;
    }

    public Map<String, Integer> getExecutorStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (String entry : auditLog) {
            // Extract executor from log entry
            // Format: [timestamp] executor ...
            int start = entry.indexOf("] ") + 2;
            int end = entry.indexOf(" ", start);
            if (start > 2 && end > start) {
                String executor = entry.substring(start, end);
                stats.put(executor, stats.getOrDefault(executor, 0) + 1);
            }
        }
        return stats;
    }

    public Map<String, Integer> getOperationStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (String entry : auditLog) {
            if (entry.contains("changed")) {
                stats.put("state_change", stats.getOrDefault("state_change", 0) + 1);
            } else if (entry.contains("executed command")) {
                stats.put("command", stats.getOrDefault("command", 0) + 1);
            } else if (entry.contains("batch operation")) {
                stats.put("batch", stats.getOrDefault("batch", 0) + 1);
            } else if (entry.contains("ERROR")) {
                stats.put("error", stats.getOrDefault("error", 0) + 1);
            }
        }
        return stats;
    }

    public void cleanupOldEntries(int daysToKeep) {
        if (daysToKeep <= 0) {
            return;
        }
        
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        int removed = 0;
        
        Iterator<String> iterator = auditLog.iterator();
        while (iterator.hasNext()) {
            String entry = iterator.next();
            try {
                // Extract timestamp from entry: [yyyy-MM-dd HH:mm:ss]
                int start = entry.indexOf("[") + 1;
                int end = entry.indexOf("]");
                if (start > 0 && end > start) {
                    String timestampStr = entry.substring(start, end);
                    Date entryDate = dateFormat.parse(timestampStr);
                    if (entryDate.getTime() < cutoffTime) {
                        iterator.remove();
                        removed++;
                    }
                }
            } catch (Exception e) {
                // If we can't parse the timestamp, keep the entry
                plugin.getLogger().warning("Could not parse timestamp in audit entry: " + entry);
            }
        }
        
        if (removed > 0) {
            flushLogs();
            plugin.getLogger().info("Cleaned up " + removed + " old audit entries");
        }
    }

    public int getTotalEntries() {
        return auditLog.size();
    }

    public String getOldestEntryDate() {
        if (auditLog.isEmpty()) {
            return "No entries";
        }
        
        try {
            String firstEntry = auditLog.get(0);
            int start = firstEntry.indexOf("[") + 1;
            int end = firstEntry.indexOf("]");
            return firstEntry.substring(start, end);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getNewestEntryDate() {
        if (auditLog.isEmpty()) {
            return "No entries";
        }
        
        try {
            String lastEntry = auditLog.get(auditLog.size() - 1);
            int start = lastEntry.indexOf("[") + 1;
            int end = lastEntry.indexOf("]");
            return lastEntry.substring(start, end);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
