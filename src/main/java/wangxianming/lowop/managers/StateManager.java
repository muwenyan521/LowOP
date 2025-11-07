package wangxianming.lowop.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import wangxianming.lowop.LowOP;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class StateManager {

    private final LowOP plugin;
    private final Map<UUID, PermissionManager.PermissionLevel> playerStates;
    private FileConfiguration statesConfig;
    private File statesFile;

    public StateManager(LowOP plugin) {
        this.plugin = plugin;
        this.playerStates = new HashMap<>();
        loadStates();
        startAutoSaveTask();
    }

    private void loadStates() {
        statesFile = new File(plugin.getDataFolder(), "player_states.yml");
        if (!statesFile.exists()) {
            try {
                statesFile.getParentFile().mkdirs();
                statesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create player_states.yml", e);
                return;
            }
        }
        
        statesConfig = YamlConfiguration.loadConfiguration(statesFile);
        
        // Load all player states from file
        if (statesConfig.contains("players")) {
            for (String uuidStr : statesConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String levelStr = statesConfig.getString("players." + uuidStr + ".level", "PLAYER");
                    PermissionManager.PermissionLevel level = PermissionManager.PermissionLevel.valueOf(levelStr);
                    playerStates.put(uuid, level);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID or permission level in player_states.yml: " + uuidStr);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + playerStates.size() + " player states");
    }

    public void saveAllData() {
        saveStates();
    }

    private void saveStates() {
        try {
            // Clear existing data
            for (String key : statesConfig.getKeys(false)) {
                statesConfig.set(key, null);
            }
            
            // Save all player states
            for (Map.Entry<UUID, PermissionManager.PermissionLevel> entry : playerStates.entrySet()) {
                statesConfig.set("players." + entry.getKey().toString() + ".level", entry.getValue().name());
            }
            
            statesConfig.save(statesFile);
            plugin.getLogger().fine("Player states saved successfully");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player states to " + statesFile, e);
        }
    }

    private void startAutoSaveTask() {
        int interval = plugin.getConfigManager().getAutoSaveInterval();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::saveStates, 
            interval * 20L, interval * 20L); // Convert seconds to ticks
    }

    // New methods for three-level permission system
    public PermissionManager.PermissionLevel setPlayerPermissionLevel(UUID playerUUID, PermissionManager.PermissionLevel level, String executor) {
        PermissionManager.PermissionLevel previousLevel = playerStates.getOrDefault(playerUUID, PermissionManager.PermissionLevel.PLAYER);
        playerStates.put(playerUUID, level);
        
        // Log the change
        if (plugin.getConfigManager().isAuditLogEnabled()) {
            plugin.getAuditManager().logPermissionLevelChange(playerUUID, previousLevel, level, executor);
        }
        
        // Auto-save if enabled
        if (plugin.getConfigManager().getAutoSaveInterval() > 0) {
            saveStates();
        }
        
        return previousLevel;
    }

    public PermissionManager.PermissionLevel getPlayerPermissionLevel(UUID playerUUID) {
        return playerStates.getOrDefault(playerUUID, PermissionManager.PermissionLevel.PLAYER);
    }

    public void removePlayerState(UUID playerUUID) {
        playerStates.remove(playerUUID);
        if (statesConfig.contains("players." + playerUUID.toString())) {
            statesConfig.set("players." + playerUUID.toString(), null);
        }
    }

    // Bulk operations for three-level system
    public int setMultiplePlayersPermissionLevel(List<UUID> playerUUIDs, PermissionManager.PermissionLevel level, String executor) {
        int count = 0;
        for (UUID uuid : playerUUIDs) {
            setPlayerPermissionLevel(uuid, level, executor);
            count++;
        }
        saveStates();
        return count;
    }

    // Async methods for offline player support
    public CompletableFuture<Boolean> setPlayerPermissionLevelAsync(UUID playerUUID, PermissionManager.PermissionLevel level, String executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First update the internal state
                setPlayerPermissionLevel(playerUUID, level, executor);
                
                // Then apply the actual permission changes through PermissionManager
                org.bukkit.command.CommandSender sender = plugin.getServer().getConsoleSender();
                return plugin.getPermissionManager().setPlayerPermissionLevel(playerUUID, level, sender).join();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to set permission level for player " + playerUUID + ": " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Integer> setMultiplePlayersPermissionLevelAsync(List<UUID> playerUUIDs, PermissionManager.PermissionLevel level, String executor) {
        return CompletableFuture.supplyAsync(() -> {
            return setMultiplePlayersPermissionLevel(playerUUIDs, level, executor);
        });
    }

    public Map<UUID, PermissionManager.PermissionLevel> getAllPlayerStates() {
        return new HashMap<>(playerStates);
    }

    public List<UUID> getPlayersWithPermissionLevel(PermissionManager.PermissionLevel level) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, PermissionManager.PermissionLevel> entry : playerStates.entrySet()) {
            if (entry.getValue() == level) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // Utility methods for three-level system
    public boolean hasPlayerState(UUID playerUUID) {
        return playerStates.containsKey(playerUUID);
    }

    public boolean hasPermissionLevel(UUID playerUUID, PermissionManager.PermissionLevel level) {
        return playerStates.getOrDefault(playerUUID, PermissionManager.PermissionLevel.PLAYER) == level;
    }

    public int getTotalPlayers() {
        return playerStates.size();
    }

    public int getPlayerCountByLevel(PermissionManager.PermissionLevel level) {
        return getPlayersWithPermissionLevel(level).size();
    }

    // Backward compatibility methods
    public boolean setPlayerAdminState(UUID playerUUID, boolean isAdmin, String executor) {
        PermissionManager.PermissionLevel level = isAdmin ? PermissionManager.PermissionLevel.OP : PermissionManager.PermissionLevel.PLAYER;
        PermissionManager.PermissionLevel previousLevel = setPlayerPermissionLevel(playerUUID, level, executor);
        return previousLevel == PermissionManager.PermissionLevel.OP;
    }

    public boolean getPlayerAdminState(UUID playerUUID) {
        return getPlayerPermissionLevel(playerUUID) == PermissionManager.PermissionLevel.OP;
    }

    public boolean togglePlayerAdminState(UUID playerUUID, String executor) {
        boolean currentState = getPlayerAdminState(playerUUID);
        return setPlayerAdminState(playerUUID, !currentState, executor);
    }

    public int setMultiplePlayersAdminState(List<UUID> playerUUIDs, boolean isAdmin, String executor) {
        PermissionManager.PermissionLevel level = isAdmin ? PermissionManager.PermissionLevel.OP : PermissionManager.PermissionLevel.PLAYER;
        return setMultiplePlayersPermissionLevel(playerUUIDs, level, executor);
    }

    public List<UUID> getPlayersWithAdminState(boolean isAdmin) {
        PermissionManager.PermissionLevel level = isAdmin ? PermissionManager.PermissionLevel.OP : PermissionManager.PermissionLevel.PLAYER;
        return getPlayersWithPermissionLevel(level);
    }

    public boolean hasAdminState(UUID playerUUID) {
        return getPlayerAdminState(playerUUID);
    }

    public int getAdminCount() {
        return getPlayerCountByLevel(PermissionManager.PermissionLevel.OP);
    }

    public void cleanupOldData(int daysOld) {
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        int removed = 0;
        
        // This would require storing last access time, which we don't currently do
        // For now, we'll just log that this feature is not implemented
        plugin.getLogger().info("Data cleanup feature requires additional implementation");
    }
}
