package wangxianming.lowop.managers;

import org.bukkit.Bukkit;
import wangxianming.lowop.LowOP;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HealthManager {

    private final LowOP plugin;
    private long lastHealthCheck;
    private final Map<String, Object> healthMetrics;

    public HealthManager(LowOP plugin) {
        this.plugin = plugin;
        this.healthMetrics = new HashMap<>();
        this.lastHealthCheck = System.currentTimeMillis();
    }

    public boolean performHealthCheck() {
        lastHealthCheck = System.currentTimeMillis();
        boolean overallHealth = true;

        try {
            // Check LuckPerms availability
            boolean luckPermsHealthy = checkLuckPermsHealth();
            healthMetrics.put("luckperms_healthy", luckPermsHealthy);
            overallHealth &= luckPermsHealthy;

            // Check file system health
            boolean fileSystemHealthy = checkFileSystemHealth();
            healthMetrics.put("filesystem_healthy", fileSystemHealthy);
            overallHealth &= fileSystemHealthy;

            // Check memory health
            boolean memoryHealthy = checkMemoryHealth();
            healthMetrics.put("memory_healthy", memoryHealthy);
            overallHealth &= memoryHealthy;

            // Check plugin state
            boolean pluginStateHealthy = checkPluginState();
            healthMetrics.put("plugin_state_healthy", pluginStateHealthy);
            overallHealth &= pluginStateHealthy;

            // Check command execution
            boolean commandExecutionHealthy = checkCommandExecution();
            healthMetrics.put("command_execution_healthy", commandExecutionHealthy);
            overallHealth &= commandExecutionHealthy;

            // Update overall health
            healthMetrics.put("overall_healthy", overallHealth);
            healthMetrics.put("last_check_timestamp", lastHealthCheck);
            healthMetrics.put("check_duration", System.currentTimeMillis() - lastHealthCheck);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Health check completed: " + (overallHealth ? "HEALTHY" : "ISSUES DETECTED"));
            }

            return overallHealth;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Health check failed with exception", e);
            healthMetrics.put("overall_healthy", false);
            healthMetrics.put("last_error", e.getMessage());
            return false;
        }
    }

    private boolean checkLuckPermsHealth() {
        try {
            org.bukkit.plugin.Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (luckPerms == null || !luckPerms.isEnabled()) {
                plugin.getLogger().warning("LuckPerms health check: Not installed or not enabled");
                return false;
            }

            // Test if we can execute a simple LuckPerms command without output
            // Use a silent command that doesn't produce console output
            boolean commandSuccess = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp info");
            if (!commandSuccess) {
                plugin.getLogger().warning("LuckPerms health check: Command execution failed");
                return false;
            }

            healthMetrics.put("luckperms_version", luckPerms.getDescription().getVersion());
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "LuckPerms health check failed", e);
            return false;
        }
    }

    private boolean checkFileSystemHealth() {
        try {
            // Check if data folder is writable
            java.io.File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().warning("File system health: Cannot create data folder");
                return false;
            }

            // Test file creation
            java.io.File testFile = new java.io.File(dataFolder, ".health_test");
            try {
                if (!testFile.createNewFile()) {
                    plugin.getLogger().warning("File system health: Cannot create test file");
                    return false;
                }
                if (!testFile.delete()) {
                    plugin.getLogger().warning("File system health: Cannot delete test file");
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "File system health: File operations failed", e);
                return false;
            }

            // Check config files
            if (!new java.io.File(dataFolder, "config.yml").exists()) {
                plugin.getLogger().warning("File system health: Config file missing");
                return false;
            }

            healthMetrics.put("data_folder_writable", true);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "File system health check failed", e);
            return false;
        }
    }

    private boolean checkMemoryHealth() {
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

            long maxHeap = heapUsage.getMax();
            long usedHeap = heapUsage.getUsed();
            long maxNonHeap = nonHeapUsage.getMax();
            long usedNonHeap = nonHeapUsage.getUsed();

            // Calculate usage percentages
            double heapUsagePercent = maxHeap > 0 ? (double) usedHeap / maxHeap * 100 : 0;
            double nonHeapUsagePercent = maxNonHeap > 0 ? (double) usedNonHeap / maxNonHeap * 100 : 0;

            healthMetrics.put("heap_used_mb", usedHeap / 1024 / 1024);
            healthMetrics.put("heap_max_mb", maxHeap / 1024 / 1024);
            healthMetrics.put("heap_usage_percent", Math.round(heapUsagePercent));
            healthMetrics.put("non_heap_used_mb", usedNonHeap / 1024 / 1024);
            healthMetrics.put("non_heap_max_mb", maxNonHeap / 1024 / 1024);
            healthMetrics.put("non_heap_usage_percent", Math.round(nonHeapUsagePercent));

            // Consider memory healthy if heap usage is below 90%
            boolean memoryHealthy = heapUsagePercent < 90;
            if (!memoryHealthy) {
                plugin.getLogger().warning("Memory health: High heap usage (" + Math.round(heapUsagePercent) + "%)");
            }

            return memoryHealthy;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Memory health check failed", e);
            return false;
        }
    }

    private boolean checkPluginState() {
        try {
            // Check if all managers are initialized
            if (plugin.getConfigManager() == null) {
                plugin.getLogger().warning("Plugin state health: ConfigManager not initialized");
                return false;
            }
            if (plugin.getStateManager() == null) {
                plugin.getLogger().warning("Plugin state health: StateManager not initialized");
                return false;
            }
            if (plugin.getPermissionManager() == null) {
                plugin.getLogger().warning("Plugin state health: PermissionManager not initialized");
                return false;
            }
            if (plugin.getAuditManager() == null) {
                plugin.getLogger().warning("Plugin state health: AuditManager not initialized");
                return false;
            }

            // Check player state count
            int playerStateCount = plugin.getStateManager().getTotalPlayers();
            healthMetrics.put("player_state_count", playerStateCount);

            // Check audit log entries
            int auditEntryCount = plugin.getAuditManager().getTotalEntries();
            healthMetrics.put("audit_entry_count", auditEntryCount);

            healthMetrics.put("managers_initialized", true);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Plugin state health check failed", e);
            return false;
        }
    }

    private boolean checkCommandExecution() {
        try {
            // Test if we can execute a simple Bukkit command
            boolean testSuccess = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "help");
            healthMetrics.put("command_execution_test", testSuccess);
            
            if (!testSuccess) {
                plugin.getLogger().warning("Command execution health: Basic command test failed");
            }
            
            return testSuccess;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Command execution health check failed", e);
            return false;
        }
    }

    public Map<String, Object> getHealthMetrics() {
        return new HashMap<>(healthMetrics);
    }

    public String getHealthSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== LowOP Health Summary ===\n");
        
        // Overall status
        boolean overallHealthy = (Boolean) healthMetrics.getOrDefault("overall_healthy", false);
        summary.append("Overall: ").append(overallHealthy ? "§aHEALTHY" : "§cISSUES DETECTED").append("§r\n");
        
        // Component status
        summary.append("LuckPerms: ").append(getStatusString("luckperms_healthy")).append("\n");
        summary.append("File System: ").append(getStatusString("filesystem_healthy")).append("\n");
        summary.append("Memory: ").append(getStatusString("memory_healthy")).append("\n");
        summary.append("Plugin State: ").append(getStatusString("plugin_state_healthy")).append("\n");
        summary.append("Commands: ").append(getStatusString("command_execution_healthy")).append("\n");
        
        // Metrics
        summary.append("\n=== Metrics ===\n");
        summary.append("Player States: ").append(healthMetrics.getOrDefault("player_state_count", 0)).append("\n");
        summary.append("Audit Entries: ").append(healthMetrics.getOrDefault("audit_entry_count", 0)).append("\n");
        
        if (healthMetrics.containsKey("heap_usage_percent")) {
            summary.append("Heap Usage: ").append(healthMetrics.get("heap_usage_percent")).append("%\n");
        }
        
        if (healthMetrics.containsKey("luckperms_version")) {
            summary.append("LuckPerms: ").append(healthMetrics.get("luckperms_version")).append("\n");
        }
        
        long lastCheck = (Long) healthMetrics.getOrDefault("last_check_timestamp", 0L);
        if (lastCheck > 0) {
            long secondsAgo = (System.currentTimeMillis() - lastCheck) / 1000;
            summary.append("Last Check: ").append(secondsAgo).append(" seconds ago\n");
        }

        return summary.toString();
    }

    private String getStatusString(String metric) {
        Object value = healthMetrics.get(metric);
        if (value instanceof Boolean) {
            return (Boolean) value ? "§a✓" : "§c✗";
        }
        return "§7?";
    }

    public long getLastHealthCheck() {
        return lastHealthCheck;
    }

    public boolean isSystemHealthy() {
        return (Boolean) healthMetrics.getOrDefault("overall_healthy", false);
    }

    public void scheduleRegularHealthChecks() {
        // Schedule health checks every 5 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Performing scheduled health check...");
            }
            performHealthCheck();
        }, 6000L, 6000L); // 5 minutes = 6000 ticks
    }
}
