package wangxianming.lowop.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wangxianming.lowop.LowOP;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PermissionManager {

    private final LowOP plugin;

    public PermissionManager(LowOP plugin) {
        this.plugin = plugin;
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
        
        // Try to get from offline player (this might not work for all cases)
        // For production, you might want to implement a more robust solution
        return Bukkit.getOfflinePlayer(playerUUID).getName();
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

    // Check permission state for a player
    public boolean checkPermissionState(String playerName, boolean expectedAdminState) {
        try {
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                return false;
            }
            
            UUID playerUUID = player.getUniqueId();
            boolean actualState = plugin.getStateManager().hasAdminState(playerUUID);
            
            return actualState == expectedAdminState;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking permission state for " + playerName, e);
            return false;
        }
    }

    // Set admin permissions for a player by name
    public CompletableFuture<Boolean> setAdminPermissions(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissions(player.getUniqueId(), true, Bukkit.getConsoleSender());
    }

    // Set default permissions for a player by name
    public CompletableFuture<Boolean> setDefaultPermissions(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissions(player.getUniqueId(), false, Bukkit.getConsoleSender());
    }

    // Set player permissions by player name (overloaded version)
    public CompletableFuture<Boolean> setPlayerPermissions(String playerName, boolean enableAdmin) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }
        return setPlayerPermissions(player.getUniqueId(), enableAdmin, Bukkit.getConsoleSender());
    }

    // Set multiple players permissions by player names (overloaded version)
    public CompletableFuture<Integer> setMultiplePlayersPermissions(java.util.List<String> playerNames, boolean enableAdmin) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        if (playerNames.isEmpty()) {
            future.complete(0);
            return future;
        }

        java.util.List<UUID> playerUUIDs = new java.util.ArrayList<>();
        for (String playerName : playerNames) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                playerUUIDs.add(player.getUniqueId());
            }
        }

        if (playerUUIDs.isEmpty()) {
            future.complete(0);
            return future;
        }

        return setMultiplePlayersPermissions(playerUUIDs, enableAdmin, Bukkit.getConsoleSender());
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
}
