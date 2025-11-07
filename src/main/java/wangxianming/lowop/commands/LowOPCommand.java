package wangxianming.lowop.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wangxianming.lowop.LowOP;
import wangxianming.lowop.managers.PermissionManager;
import wangxianming.lowop.utils.MessageUtils;
import wangxianming.lowop.utils.ValidationUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LowOPCommand implements CommandExecutor {

    private final LowOP plugin;
    private final MessageUtils messageUtils;
    private final ValidationUtils validationUtils;

    public LowOPCommand(LowOP plugin) {
        this.plugin = plugin;
        this.messageUtils = new MessageUtils(plugin.getConfigManager());
        this.validationUtils = new ValidationUtils();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("lowop.manage")) {
            messageUtils.sendMessage(sender, "no-permission");
            return true;
        }

        // Handle different subcommands
        if (args.length == 0) {
            return handleHelp(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                return handleHelp(sender);
            case "reload":
                return handleReload(sender);
            case "status":
                return handleStatus(sender, args);
            case "health":
                return handleHealth(sender);
            case "audit":
                return handleAudit(sender, args);
            case "batch":
                return handleBatch(sender, args);
            case "version":
                return handleVersion(sender);
            case "detect":
                return handleDetect(sender, args);
            default:
                // Assume it's a player selector for permission management
                return handlePlayerPermission(sender, args);
        }
    }

    private boolean handleHelp(CommandSender sender) {
        List<String> helpMessages = plugin.getConfigManager().getMessageList("help-message", Arrays.asList(
            "§6=== LowOP Help ===",
            "§e/lowop <player> [player|lowop|op|status] §7- 管理玩家权限级别",
            "§e/lowop batch <player|lowop|op> <player1,player2,...> §7- 批量设置权限级别",
            "§e/lowop status [player] §7- 查看权限状态",
            "§e/lowop detect <player> §7- 检测玩家权限级别",
            "§e/lowop reload §7- 重载配置",
            "§e/lowop health §7- 系统健康检查",
            "§e/lowop audit [page] §7- 查看审计日志",
            "§e/lowop version §7- 版本信息",
            "§e/lowop help §7- 显示此帮助",
            "§7玩家选择符: §e@a §7(所有玩家), §e@p §7(最近玩家), §e@s §7(自己)"
        ));
        
        for (String message : helpMessages) {
            sender.sendMessage(message);
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfigs();
            messageUtils.sendMessage(sender, "config-reloaded");
            plugin.getAuditManager().logCommandExecution("reload", getExecutorName(sender), true);
            return true;
        } catch (Exception e) {
            messageUtils.sendMessage(sender, "config-reload-error");
            plugin.getAuditManager().logError("reload", getExecutorName(sender), e.getMessage());
            return true;
        }
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Show overall status
            int totalPlayers = plugin.getStateManager().getTotalPlayers();
            int playerCount = plugin.getStateManager().getPlayerCountByLevel(PermissionManager.PermissionLevel.PLAYER);
            int lowopCount = plugin.getStateManager().getPlayerCountByLevel(PermissionManager.PermissionLevel.LOWOP);
            int opCount = plugin.getStateManager().getPlayerCountByLevel(PermissionManager.PermissionLevel.OP);
            
            messageUtils.sendMessage(sender, "status-overall", Map.of(
                "total", String.valueOf(totalPlayers),
                "players", String.valueOf(playerCount),
                "lowops", String.valueOf(lowopCount),
                "ops", String.valueOf(opCount)
            ));
            return true;
        }

        // Show specific player status
        String playerSelector = args[1];
        List<UUID> playerUUIDs = validationUtils.parsePlayerSelector(playerSelector, sender);
        
        if (playerUUIDs.isEmpty()) {
            messageUtils.sendMessage(sender, "player-not-found", Map.of("player", playerSelector));
            return true;
        }

        for (UUID playerUUID : playerUUIDs) {
            String playerName = getPlayerName(playerUUID);
            PermissionManager.PermissionLevel level = plugin.getStateManager().getPlayerPermissionLevel(playerUUID);
            String status = getPermissionLevelDisplay(level);
            
            messageUtils.sendMessage(sender, "status-player", Map.of(
                "player", playerName,
                "status", status
            ));
        }
        return true;
    }

    private boolean handleHealth(CommandSender sender) {
        boolean healthy = plugin.getHealthManager().performHealthCheck();
        String healthSummary = plugin.getHealthManager().getHealthSummary();
        
        // Send health summary line by line
        for (String line : healthSummary.split("\n")) {
            sender.sendMessage(line);
        }
        
        plugin.getAuditManager().logCommandExecution("health", getExecutorName(sender), healthy);
        return true;
    }

    private boolean handleAudit(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                messageUtils.sendMessage(sender, "invalid-page");
                return true;
            }
        }

        List<String> recentEntries = plugin.getAuditManager().getRecentEntries(10);
        if (recentEntries.isEmpty()) {
            messageUtils.sendMessage(sender, "no-audit-entries");
            return true;
        }

        sender.sendMessage("§6=== 最近审计记录 (第 " + page + " 页) ===");
        for (int i = 0; i < Math.min(recentEntries.size(), 10); i++) {
            sender.sendMessage("§7" + (i + 1) + ". §f" + recentEntries.get(i));
        }
        
        plugin.getAuditManager().logCommandExecution("audit", getExecutorName(sender), true);
        return true;
    }

    private boolean handleBatch(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageUtils.sendMessage(sender, "batch-usage");
            return true;
        }

        String levelStr = args[1].toLowerCase();
        if (!validationUtils.validatePermissionLevel(levelStr, sender, messageUtils)) {
            return true;
        }

        PermissionManager.PermissionLevel level = validationUtils.parsePermissionLevel(levelStr);
        String playerList = args[2];
        String[] playerSelectors = playerList.split(",");

        if (playerSelectors.length > 10) {
            messageUtils.sendMessage(sender, "batch-too-many-players");
            return true;
        }

        // Validate all players first
        List<UUID> validPlayerUUIDs = validationUtils.validatePlayerListWithSelectors(Arrays.asList(playerSelectors), sender);
        if (validPlayerUUIDs.isEmpty()) {
            messageUtils.sendMessage(sender, "no-valid-players");
            return true;
        }

        messageUtils.sendMessage(sender, "batch-processing", Map.of(
            "count", String.valueOf(validPlayerUUIDs.size()),
            "operation", getPermissionLevelDisplay(level)
        ));

        // Process batch operation
        plugin.getStateManager().setMultiplePlayersPermissionLevelAsync(validPlayerUUIDs, level, getExecutorName(sender))
            .thenAccept(successCount -> {
                messageUtils.sendMessage(sender, "batch-completed", Map.of(
                    "success", String.valueOf(successCount),
                    "total", String.valueOf(validPlayerUUIDs.size())
                ));
                
                plugin.getAuditManager().logBatchPermissionLevelChange(getExecutorName(sender), 
                    validPlayerUUIDs.size(), successCount, level);
            });

        return true;
    }

    private boolean handleVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        
        sender.sendMessage("§6=== LowOP 版本信息 ===");
        sender.sendMessage("§7版本: §e" + version);
        sender.sendMessage("§7作者: §e" + authors);
        sender.sendMessage("§7Minecraft: §e1.21");
        sender.sendMessage("§7LuckPerms: §e" + plugin.getPermissionManager().getLuckPermsVersion());
        return true;
    }

    private boolean handleDetect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageUtils.sendMessage(sender, "detect-usage");
            return true;
        }

        String playerSelector = args[1];
        List<UUID> playerUUIDs = validationUtils.parsePlayerSelector(playerSelector, sender);
        
        if (playerUUIDs.isEmpty()) {
            messageUtils.sendMessage(sender, "player-not-found", Map.of("player", playerSelector));
            return true;
        }

        messageUtils.sendMessage(sender, "detect-processing", Map.of("player", playerSelector));

        for (UUID playerUUID : playerUUIDs) {
            String playerName = getPlayerName(playerUUID);
            
            plugin.getPermissionManager().detectPlayerPermissionLevel(playerName)
                .thenAccept(level -> {
                    String detectedLevel = getPermissionLevelDisplay(level);
                    messageUtils.sendMessage(sender, "detect-result", Map.of(
                        "player", playerName,
                        "level", detectedLevel
                    ));
                    
                    plugin.getAuditManager().logPermissionDetection(getExecutorName(sender), playerName, level);
                });
        }

        return true;
    }

    private boolean handlePlayerPermission(CommandSender sender, String[] args) {
        String playerSelector = args[0];
        List<UUID> playerUUIDs = validationUtils.parsePlayerSelector(playerSelector, sender);
        
        if (playerUUIDs.isEmpty()) {
            messageUtils.sendMessage(sender, "player-not-found", Map.of("player", playerSelector));
            return true;
        }

        PermissionManager.PermissionLevel targetLevel;
        if (args.length == 1) {
            // Show current status
            for (UUID playerUUID : playerUUIDs) {
                String playerName = getPlayerName(playerUUID);
                PermissionManager.PermissionLevel currentLevel = plugin.getStateManager().getPlayerPermissionLevel(playerUUID);
                String status = getPermissionLevelDisplay(currentLevel);
                
                messageUtils.sendMessage(sender, "status-player", Map.of(
                    "player", playerName,
                    "status", status
                ));
            }
            return true;
        } else {
            String levelStr = args[1].toLowerCase();
            if (!validationUtils.validatePermissionLevel(levelStr, sender, messageUtils)) {
                return true;
            }
            targetLevel = validationUtils.parsePermissionLevel(levelStr);
        }

        // Apply rate limiting
        if (!validationUtils.checkRateLimit(sender)) {
            messageUtils.sendMessage(sender, "rate-limit-exceeded");
            return true;
        }

        // Execute permission change
        messageUtils.sendMessage(sender, "processing-request", Map.of("player", playerSelector));
        
        for (UUID playerUUID : playerUUIDs) {
            String playerName = getPlayerName(playerUUID);
            PermissionManager.PermissionLevel currentLevel = plugin.getStateManager().getPlayerPermissionLevel(playerUUID);
            
            plugin.getStateManager().setPlayerPermissionLevelAsync(playerUUID, targetLevel, getExecutorName(sender))
                .thenAccept(success -> {
                    if (success) {
                        String messageKey = getPermissionChangeMessageKey(currentLevel, targetLevel);
                        messageUtils.sendMessage(sender, messageKey, Map.of(
                            "player", playerName,
                            "from", getPermissionLevelDisplay(currentLevel),
                            "to", getPermissionLevelDisplay(targetLevel)
                        ));
                    } else {
                        messageUtils.sendMessage(sender, "operation-failed", Map.of("player", playerName));
                    }
                });
        }

        return true;
    }

    private String getPermissionLevelDisplay(PermissionManager.PermissionLevel level) {
        switch (level) {
            case PLAYER:
                return plugin.getConfigManager().getMessage("player-status", "§7普通玩家");
            case LOWOP:
                return plugin.getConfigManager().getMessage("lowop-status", "§e低权限OP");
            case OP:
                return plugin.getConfigManager().getMessage("op-status", "§a管理员");
            default:
                return "§7未知";
        }
    }

    private String getPermissionChangeMessageKey(PermissionManager.PermissionLevel from, PermissionManager.PermissionLevel to) {
        if (from == to) {
            return "permission-unchanged";
        }
        
        if (to == PermissionManager.PermissionLevel.OP) {
            return "permission-to-op";
        } else if (to == PermissionManager.PermissionLevel.LOWOP) {
            return "permission-to-lowop";
        } else {
            return "permission-to-player";
        }
    }

    private String getPlayerName(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }
        
        // 支持离线玩家
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getName();
        }
        
        return "未知玩家";
    }

    private String getExecutorName(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
    }
}
