package wangxianming.lowop.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wangxianming.lowop.LowOP;
import wangxianming.lowop.utils.MessageUtils;
import wangxianming.lowop.utils.ValidationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
            default:
                // Assume it's a player name for permission management
                return handlePlayerPermission(sender, args);
        }
    }

    private boolean handleHelp(CommandSender sender) {
        List<String> helpMessages = plugin.getConfigManager().getMessageList("messages.help", Arrays.asList(
            "§6=== LowOP Help ===",
            "§e/lowop <player> [on|off|status] §7- 管理玩家权限",
            "§e/lowop batch <on|off> <player1,player2,...> §7- 批量操作",
            "§e/lowop status [player] §7- 查看权限状态",
            "§e/lowop reload §7- 重载配置",
            "§e/lowop health §7- 系统健康检查",
            "§e/lowop audit [page] §7- 查看审计日志",
            "§e/lowop version §7- 版本信息",
            "§e/lowop help §7- 显示此帮助"
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
            int adminCount = plugin.getStateManager().getAdminCount();
            
            messageUtils.sendMessage(sender, "status-overall", java.util.Map.of(
                "total", String.valueOf(totalPlayers),
                "admins", String.valueOf(adminCount)
            ));
            return true;
        }

        // Show specific player status
        String playerName = args[1];
        UUID playerUUID = validationUtils.getPlayerUUID(playerName);
        
        if (playerUUID == null) {
            messageUtils.sendMessage(sender, "player-not-found", java.util.Map.of("player", playerName));
            return true;
        }

        boolean isAdmin = plugin.getStateManager().getPlayerAdminState(playerUUID);
        String status = isAdmin ? 
            plugin.getConfigManager().getMessage("messages.admin-status", "§a管理员") :
            plugin.getConfigManager().getMessage("messages.default-status", "§7普通玩家");
        
        messageUtils.sendMessage(sender, "status-player", java.util.Map.of(
            "player", playerName,
            "status", status
        ));
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

        String operation = args[1].toLowerCase();
        if (!operation.equals("on") && !operation.equals("off")) {
            messageUtils.sendMessage(sender, "invalid-operation");
            return true;
        }

        boolean enableAdmin = operation.equals("on");
        String playerList = args[2];
        String[] playerNames = playerList.split(",");

        if (playerNames.length > 10) {
            messageUtils.sendMessage(sender, "batch-too-many-players");
            return true;
        }

        // Validate all players first
        List<UUID> validPlayerUUIDs = validationUtils.validatePlayerList(Arrays.asList(playerNames));
        if (validPlayerUUIDs.isEmpty()) {
            messageUtils.sendMessage(sender, "no-valid-players");
            return true;
        }

        messageUtils.sendMessage(sender, "batch-processing", java.util.Map.of(
            "count", String.valueOf(validPlayerUUIDs.size()),
            "operation", enableAdmin ? "启用" : "禁用"
        ));

        // Process batch operation
        plugin.getPermissionManager().setMultiplePlayersPermissions(validPlayerUUIDs, enableAdmin, sender)
            .thenAccept(successCount -> {
        messageUtils.sendMessage(sender, "batch-completed", java.util.Map.of(
            "success", String.valueOf(successCount),
            "total", String.valueOf(validPlayerUUIDs.size())
        ));
                
                plugin.getAuditManager().logBatchOperation("batch " + operation, getExecutorName(sender), 
                    validPlayerUUIDs.size(), successCount);
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

    private boolean handlePlayerPermission(CommandSender sender, String[] args) {
        String playerName = args[0];
        UUID playerUUID = validationUtils.getPlayerUUID(playerName);
        
        if (playerUUID == null) {
            messageUtils.sendMessage(sender, "player-not-found", java.util.Map.of("player", playerName));
            return true;
        }

        boolean enableAdmin;
        if (args.length == 1) {
            // Toggle mode
            boolean currentState = plugin.getStateManager().getPlayerAdminState(playerUUID);
            enableAdmin = !currentState;
        } else {
            String operation = args[1].toLowerCase();
            if (operation.equals("on")) {
                enableAdmin = true;
            } else if (operation.equals("off")) {
                enableAdmin = false;
            } else if (operation.equals("status")) {
                return handleStatus(sender, new String[]{"status", playerName});
            } else {
                messageUtils.sendMessage(sender, "invalid-operation");
                return true;
            }
        }

        // Apply rate limiting
        if (!validationUtils.checkRateLimit(sender)) {
            messageUtils.sendMessage(sender, "rate-limit-exceeded");
            return true;
        }

        // Execute permission change
        messageUtils.sendMessage(sender, "processing-request", java.util.Map.of("player", playerName));
        
        plugin.getPermissionManager().setPlayerPermissions(playerUUID, enableAdmin, sender)
            .thenAccept(success -> {
                if (success) {
                    String messageKey = enableAdmin ? "admin-enabled-executor" : "admin-disabled-executor";
                    messageUtils.sendMessage(sender, messageKey, java.util.Map.of("player", playerName));
                } else {
                    messageUtils.sendMessage(sender, "operation-failed", java.util.Map.of("player", playerName));
                }
            });

        return true;
    }

    private String getExecutorName(CommandSender sender) {
        return sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
    }
}
