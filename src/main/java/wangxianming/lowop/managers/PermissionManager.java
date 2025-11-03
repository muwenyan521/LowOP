package wangxianming.lowop.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wangxianming.lowop.LowOP;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PermissionManager {
    
    public enum PermissionLevel {
        PLAYER,
        LOWOP,
        OP
    }

    private final LowOP plugin;

    public PermissionManager(LowOP plugin) {
        this.plugin = plugin;
    }

    // New methods for three-level permission system
    public CompletableFuture<Boolean> setPlayerPermissionLevel(UUID playerUUID, PermissionLevel level, CommandSender executor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String playerName = getPlayerName(playerUUID);
        if (playerName == null) {
            future.complete(false);
            return future;
        }

        // Run permission changes asynchronously to avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean success = executePermissionCommands(playerName, level, executor);
                
                // Switch back to main thread for completion
                Bukkit.getScheduler().runTask(plugin, () -> {
                    future.complete(success);
                    
                    if (success) {
                        // Update state in main thread
                        plugin.getStateManager().setPlayerPermissionLevel(playerUUID, level, 
                            executor instanceof Player ? ((Player) executor).getName() : "CONSOLE");
                        
                        // Send message to player if online
                        sendPlayerLevelMessage(playerUUID, level);
                        
                        // Log successful operation
                        plugin.getLogger().info("Successfully set " + playerName + " to " + level + " permissions");
                    } else {
                        plugin.getLogger().warning("Failed to set " + playerName + " to " + level + " permissions");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing permission commands for " + playerName, e);
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(false));
            }
        });

        return future;
    }

    private boolean executePermissionCommands(String playerName, PermissionLevel level, CommandSender executor) {
        try {
            // Clear existing permissions first
            String clearCommand = "lp user " + playerName + " parent clear";
            if (!dispatchCommand(clearCommand, executor)) {
                plugin.getLogger().warning("Failed to clear permissions for " + playerName);
                return false;
            }

            // Add the appropriate group based on level
            String groupToAdd = getGroupForLevel(level);
            String addCommand = "lp user " + playerName + " parent add " + groupToAdd;
            if (!dispatchCommand(addCommand, executor)) {
                plugin.getLogger().warning("Failed to add group " + groupToAdd + " for " + playerName);
                return false;
            }

            // Apply changes
            String applyCommand = "lp applyedits";
            if (!dispatchCommand(applyCommand, executor)) {
                plugin.getLogger().warning("Failed to apply permission edits for " + playerName);
                return false;
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Exception while executing permission commands for " + playerName, e);
            return false;
        }
    }

    private String getGroupForLevel(PermissionLevel level) {
        switch (level) {
            case OP:
                return plugin.getConfigManager().getOPGroup();
            case LOWOP:
                return plugin.getConfigManager().getLowOPGroup();
            case PLAYER:
            default:
                return plugin.getConfigManager().getPlayerGroup();
        }
    }

    // Detect player's permission level from LuckPerms
    public CompletableFuture<PermissionLevel> detectPlayerPermissionLevel(String playerName) {
        CompletableFuture<PermissionLevel> future = new CompletableFuture<>();
        
        if (!isLuckPermsAvailable()) {
            future.complete(PermissionLevel.PLAYER);
            return future;
        }

        // Run detection asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PermissionLevel level = executePermissionDetection(playerName);
                
                // Switch back to main thread for completion
                Bukkit.getScheduler().runTask(plugin, () -> {
                    future.complete(level);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error detecting permission level for " + playerName, e);
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(PermissionLevel.PLAYER));
            }
        });

        return future;
    }

    private PermissionLevel executePermissionDetection(String playerName) {
        try {
            // Check if player is in OP group
            String checkOPCommand = "lp user " + playerName + " parent check " + plugin.getConfigManager().getOPGroup();
            boolean isOP = dispatchCommand(checkOPCommand, Bukkit.getConsoleSender());
            
            if (isOP) {
                return PermissionLevel.OP;
            }

            // Check if player is in LOWOP group
            String checkLowOPCommand = "lp user " + playerName + " parent check " + plugin.getConfigManager().getLowOPGroup();
            boolean isLowOP = dispatchCommand(checkLowOPCommand, Bukkit.getConsoleSender());
            
            if (isLowOP) {
                return PermissionLevel.LOWOP;
            }

            // Default to PLAYER
            return PermissionLevel.PLAYER;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Exception while detecting permission level for " + playerName, e);
            return PermissionLevel.PLAYER;
        }
    }

    public CompletableFuture<Boolean> setPlayerPermissions(UUID playerUUID, boolean enableAdmin, CommandSender executor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        String playerName = getPlayerName(playerUUID);
        if (playerName == null) {
            future.complete(false);
            return future;
        }

        String adminGroup = plugin.getConfigManager().getAdminGroup();
        String defaultGroup = plugin.getConfigManager().getDefaultGroup();

        // Run permission changes asynchronously to avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean success = executePermissionCommands(playerName, enableAdmin, adminGroup, defaultGroup, executor);
                
                // Switch back to main thread for completion
                Bukkit.getScheduler().runTask(plugin, () -> {
                    future.complete(success);
                    
                    if (success) {
                        // Update state in main thread
                        plugin.getStateManager().setPlayerAdminState(playerUUID, enableAdmin, 
                            executor instanceof Player ? ((Player) executor).getName() : "CONSOLE");
                        
                        // Send message to player if online
                        sendPlayerMessage(playerUUID, enableAdmin);
                        
                        // Log successful operation
                        plugin.getLogger().info("Successfully " + (enableAdmin ? "enabled" : "disabled") + 
                            " admin permissions for " + playerName);
                    } else {
                        plugin.getLogger().warning("Failed to " + (enableAdmin ? "enable" : "disable") + 
                            " admin permissions for " + playerName);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing permission commands for " + playerName, e);
                Bukkit.getScheduler().runTask(plugin, () -> future.complete(false));
            }
        });

        return future;
    }

    private boolean executePermissionCommands(String playerName, boolean enableAdmin, 
                                            String adminGroup, String defaultGroup, CommandSender executor) {
        try {
            // Clear existing permissions first
            String clearCommand = "lp user " + playerName + " parent clear";
            if (!dispatchCommand(clearCommand, executor)) {
                plugin.getLogger().warning("Failed to clear permissions for " + playerName);
                return false;
            }

            // Add the appropriate group
            String groupToAdd = enableAdmin ? adminGroup : defaultGroup;
            String addCommand = "lp user " + playerName + " parent add " + groupToAdd;
            if (!dispatchCommand(addCommand, executor)) {
                plugin.getLogger().warning("Failed to add group " + groupToAdd + " for " + playerName);
                return false;
            }

            // Apply changes
            String applyCommand = "lp applyedits";
            if (!dispatchCommand(applyCommand, executor)) {
                plugin.getLogger().warning("Failed to apply permission edits for " + playerName);
                return false;
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Exception while executing permission commands for " + playerName, e);
            return false;
        }
    }

    private boolean dispatchCommand(String command, CommandSender executor) {
        try {
            // Use the server to dispatch the command
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Executed command: " + command + " - Success: " + success);
            }
            
            return success;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to execute command: " + command, e);
            return false;
        }
    }

    private String getPlayerName(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }
        
        // Get from offline player
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getName();
        }
        
        return null;
    }

    /**
     * 获取玩家名称（支持离线玩家）
     */
    public String getPlayerNameWithOfflineSupport(UUID playerUUID) {
        return getPlayerName(playerUUID);
    }

    /**
     * 检查玩家是否存在（支持离线玩家）
     */
    public boolean playerExists(String playerName) {
        // Check online players first
        if (Bukkit.getPlayerExact(playerName) != null) {
            return true;
        }
        
        // Check offline players
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        return offlinePlayer.hasPlayedBefore();
    }

    /**
     * 获取玩家UUID（支持离线玩家）
     */
    public UUID getPlayerUUID(String playerName) {
        // Check online players first
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        // Check offline players
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }
        
        return null;
    }

    private void sendPlayerMessage(UUID playerUUID, boolean enableAdmin) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            String message = enableAdmin ? 
                plugin.getConfigManager().getMessage("messages.admin-enabled", "§a你已成为服务器管理员。") :
                plugin.getConfigManager().getMessage("messages.admin-disabled", "§c你的管理员权限已被移除");
            player.sendMessage(message);
        }
    }

    private void sendPlayerLevelMessage(UUID playerUUID, PermissionLevel level) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            String messageKey;
            switch (level) {
                case OP:
                    messageKey = "messages.op-status";
                    break;
                case LOWOP:
                    messageKey = "messages.lowop-status";
                    break;
                case PLAYER:
                default:
                    messageKey = "messages.player-status";
                    break;
            }
            String message = plugin.getConfigManager().getMessage(messageKey, getDefaultLevelMessage(level));
            player.sendMessage(message);
        }
    }

    private String getDefaultLevelMessage(PermissionLevel level) {
        switch (level) {
            case OP:
                return "§6你的权限等级已设置为OP";
            case LOWOP:
                return "§e你的权限等级已设置为LOWOP";
            case PLAYER:
            default:
                return "§a你的权限等级已设置为PLAYER";
        }
    }

    // Utility methods for checking LuckPerms availability
    public boolean isLuckPermsAvailable() {
        return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    public String getLuckPermsVersion() {
        org.bukkit.plugin.Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        return luckPerms != null ? luckPerms.getDescription().getVersion() : "Not installed";
    }

    // Health check method
    public boolean performHealthCheck() {
        if (!isLuckPermsAvailable()) {
            plugin.getLogger().warning("LuckPerms is not installed or not enabled!");
            return false;
        }

        // Test if we can execute a simple LuckPerms command
        try {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp version");
            if (!success) {
                plugin.getLogger().warning("LuckPerms command execution test failed");
                return false;
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "LuckPerms health check failed", e);
            return false;
        }
    }

    // Check permission state for a player (支持离线玩家)
    public boolean checkPermissionState(String playerName, boolean expectedAdminState) {
        try {
            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                return false;
            }
            
            boolean actualState = plugin.getStateManager().hasAdminState(playerUUID);
            
            return actualState == expectedAdminState;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking permission state for " + playerName, e);
            return false;
        }
    }

    // Check if player has correct permission level state (支持离线玩家)
    public boolean checkPermissionLevelState(String playerName, PermissionLevel expectedLevel) {
        try {
            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID == null) {
                return false;
            }
            
            PermissionLevel actualLevel = plugin.getStateManager().getPlayerPermissionLevel(playerUUID);
            
            return actualLevel == expectedLevel;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking permission level state for " + playerName, e);
            return false;
        }
    }

    // Set admin permissions for a player by name (支持离线玩家)
    public CompletableFuture<Boolean> setAdminPermissions(String playerName) {
        UUID playerUUID = getPlayerUUID(playerName);
        if (playerUUID == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissions(playerUUID, true, Bukkit.getConsoleSender());
    }

    // Set default permissions for a player by name (支持离线玩家)
    public CompletableFuture<Boolean> setDefaultPermissions(String playerName) {
        UUID playerUUID = getPlayerUUID(playerName);
        if (playerUUID == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissions(playerUUID, false, Bukkit.getConsoleSender());
    }

    // Set player permissions by player name (overloaded version, 支持离线玩家)
    public CompletableFuture<Boolean> setPlayerPermissions(String playerName, boolean enableAdmin) {
        UUID playerUUID = getPlayerUUID(playerName);
        if (playerUUID == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissions(playerUUID, enableAdmin, Bukkit.getConsoleSender());
    }

    // Set player permission level by player name (支持离线玩家)
    public CompletableFuture<Boolean> setPlayerPermissionLevel(String playerName, PermissionLevel level, CommandSender executor) {
        UUID playerUUID = getPlayerUUID(playerName);
        if (playerUUID == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissionLevel(playerUUID, level, executor);
    }

    // Set multiple players permissions by player names (支持离线玩家)
    public CompletableFuture<Integer> setMultiplePlayersPermissions(java.util.List<String> playerNames, boolean enableAdmin) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        if (playerNames.isEmpty()) {
            future.complete(0);
            return future;
        }

        java.util.List<UUID> playerUUIDs = new java.util.ArrayList<>();
        for (String playerName : playerNames) {
            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID != null) {
                playerUUIDs.add(playerUUID);
            }
        }

        if (playerUUIDs.isEmpty()) {
            future.complete(0);
            return future;
        }

        return setMultiplePlayersPermissions(playerUUIDs, enableAdmin, Bukkit.getConsoleSender());
    }

    // Set multiple players permission levels by player names (支持离线玩家)
    public CompletableFuture<Integer> setMultiplePlayersPermissionLevels(java.util.List<String> playerNames, PermissionLevel level, CommandSender executor) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        if (playerNames.isEmpty()) {
            future.complete(0);
            return future;
        }

        java.util.List<UUID> playerUUIDs = new java.util.ArrayList<>();
        for (String playerName : playerNames) {
            UUID playerUUID = getPlayerUUID(playerName);
            if (playerUUID != null) {
                playerUUIDs.add(playerUUID);
            }
        }

        if (playerUUIDs.isEmpty()) {
            future.complete(0);
            return future;
        }

        return setMultiplePlayersPermissionLevels(playerUUIDs, level, executor);
    }

    // Batch operations for permission levels
    public CompletableFuture<Integer> setMultiplePlayersPermissionLevels(java.util.List<UUID> playerUUIDs, 
                                                                        PermissionLevel level, CommandSender executor) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        if (playerUUIDs.isEmpty()) {
            future.complete(0);
            return future;
        }

        // Process players sequentially to avoid overwhelming the server
        processPlayersSequentiallyForLevels(playerUUIDs, level, executor, future, 0, 0);
        
        return future;
    }

    // Batch operations
    public CompletableFuture<Integer> setMultiplePlayersPermissions(java.util.List<UUID> playerUUIDs, 
                                                                   boolean enableAdmin, CommandSender executor) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        if (playerUUIDs.isEmpty()) {
            future.complete(0);
            return future;
        }

        // Process players sequentially to avoid overwhelming the server
        processPlayersSequentially(playerUUIDs, enableAdmin, executor, future, 0, 0);
        
        return future;
    }

    private void processPlayersSequentially(java.util.List<UUID> playerUUIDs, boolean enableAdmin, 
                                          CommandSender executor, CompletableFuture<Integer> future, 
                                          int index, int successCount) {
        if (index >= playerUUIDs.size()) {
            future.complete(successCount);
            return;
        }

        UUID playerUUID = playerUUIDs.get(index);
        setPlayerPermissions(playerUUID, enableAdmin, executor).thenAccept(success -> {
            int newSuccessCount = success ? successCount + 1 : successCount;
            
            // Process next player after a short delay to avoid overwhelming the server
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                processPlayersSequentially(playerUUIDs, enableAdmin, executor, future, index + 1, newSuccessCount);
            }, 2L); // 2 tick delay between operations
        });
    }

    private void processPlayersSequentiallyForLevels(java.util.List<UUID> playerUUIDs, PermissionLevel level, 
                                                   CommandSender executor, CompletableFuture<Integer> future, 
                                                   int index, int successCount) {
        if (index >= playerUUIDs.size()) {
            future.complete(successCount);
            return;
        }

        UUID playerUUID = playerUUIDs.get(index);
        setPlayerPermissionLevel(playerUUID, level, executor).thenAccept(success -> {
            int newSuccessCount = success ? successCount + 1 : successCount;
            
            // Process next player after a short delay to avoid overwhelming the server
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                processPlayersSequentiallyForLevels(playerUUIDs, level, executor, future, index + 1, newSuccessCount);
            }, 2L); // 2 tick delay between operations
        });
    }
}
