package wangxianming.lowop.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import wangxianming.lowop.LowOP;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class StateManager {

    private final LowOP plugin;
    private final Map<UUID, Boolean> playerStates;
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
                    boolean isAdmin = statesConfig.getBoolean("players." + uuidStr + ".admin", false);
                    playerStates.put(uuid, isAdmin);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player_states.yml: " + uuidStr);
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
            for (Map.Entry<UUID, Boolean> entry : playerStates.entrySet()) {
                statesConfig.set("players." + entry.getKey().toString() + ".admin", entry.getValue());
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

    // State management methods
    public boolean setPlayerAdminState(UUID playerUUID, boolean isAdmin, String executor) {
        boolean previousState = playerStates.getOrDefault(playerUUID, false);
        playerStates.put(playerUUID, isAdmin);
        
        // Log the change
        if (plugin.getConfigManager().isAuditLogEnabled()) {
            plugin.getAuditManager().logStateChange(playerUUID, previousState, isAdmin, executor);
        }
        
        // Auto-save if enabled
        if (plugin.getConfigManager().getAutoSaveInterval() > 0) {
            saveStates();
        }
        
        return previousState;
    }

    public boolean getPlayerAdminState(UUID playerUUID) {
        return playerStates.getOrDefault(playerUUID, false);
    }

    public boolean togglePlayerAdminState(UUID playerUUID, String executor) {
        boolean currentState = getPlayerAdminState(playerUUID);
        return setPlayerAdminState(playerUUID, !currentState, executor);
    }

    public void removePlayerState(UUID playerUUID) {
        playerStates.remove(playerUUID);
        if (statesConfig.contains("players." + playerUUID.toString())) {
            statesConfig.set("players." + playerUUID.toString(), null);
        }
    }

    // Bulk operations
    public int setMultiplePlayersAdminState(List<UUID> playerUUIDs, boolean isAdmin, String executor) {
        int count = 0;
        for (UUID uuid : playerUUIDs) {
            if (setPlayerAdminState(uuid, isAdmin, executor)) {
                count++;
            }
        }
        saveStates();
        return count;
    }

    public Map<UUID, Boolean> getAllPlayerStates() {
        return new HashMap<>(playerStates);
    }

    public List<UUID> getPlayersWithAdminState(boolean isAdmin) {
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Boolean> entry : playerStates.entrySet()) {
            if (entry.getValue() == isAdmin) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // Utility methods
    public boolean hasPlayerState(UUID playerUUID) {
        return playerStates.containsKey(playerUUID);
    }

    public boolean hasAdminState(UUID playerUUID) {
        return playerStates.getOrDefault(playerUUID, false);
    }

    public int getTotalPlayers() {
        return playerStates.size();
    }

    public int getAdminCount() {
        return getPlayersWithAdminState(true).size();
    }

    public void cleanupOldData(int daysOld) {
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        int removed = 0;
        
        // This would require storing last access time, which we don't currently do
        // For now, we'll just log that this feature is not implemented
        plugin.getLogger().info("Data cleanup feature requires additional implementation");
    }
}
