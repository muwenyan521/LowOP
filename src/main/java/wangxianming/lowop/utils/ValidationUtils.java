package wangxianming.lowop.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 验证工具类，提供输入验证和权限检查功能
 */
public class ValidationUtils {
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);
    
    /**
     * 验证玩家名称格式
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * 验证 UUID 格式
     */
    public static boolean isValidUUID(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }
    
    /**
     * 检查玩家是否在线
     */
    public static boolean isPlayerOnline(String playerName) {
        return Bukkit.getPlayerExact(playerName) != null;
    }
    
    /**
     * 检查玩家是否在线（通过 UUID）
     */
    public static boolean isPlayerOnline(UUID playerUUID) {
        return Bukkit.getPlayer(playerUUID) != null;
    }
    
    /**
     * 获取在线玩家（通过名称）
     */
    public static Player getOnlinePlayer(String playerName) {
        return Bukkit.getPlayerExact(playerName);
    }
    
    /**
     * 获取在线玩家（通过 UUID）
     */
    public static Player getOnlinePlayer(UUID playerUUID) {
        return Bukkit.getPlayer(playerUUID);
    }
    
    /**
     * 检查发送者是否有权限
     */
    public static boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.isOp();
    }
    
    /**
     * 检查发送者是否有管理权限
     */
    public static boolean hasManagePermission(CommandSender sender) {
        return hasPermission(sender, "lowop.manage");
    }
    
    /**
     * 检查发送者是否有批量操作权限
     */
    public static boolean hasBatchPermission(CommandSender sender) {
        return hasPermission(sender, "lowop.batch");
    }
    
    /**
     * 检查发送者是否有审计权限
     */
    public static boolean hasAuditPermission(CommandSender sender) {
        return hasPermission(sender, "lowop.audit");
    }
    
    /**
     * 验证命令参数数量
     */
    public static boolean validateArgsCount(CommandSender sender, String[] args, int min, int max, MessageUtils messageUtils) {
        if (args.length < min) {
            messageUtils.sendError(sender, "参数不足。用法: /lowop <玩家名> [on/off]");
            return false;
        }
        if (args.length > max) {
            messageUtils.sendError(sender, "参数过多。用法: /lowop <玩家名> [on/off]");
            return false;
        }
        return true;
    }
    
    /**
     * 验证状态参数（on/off）
     */
    public static boolean validateStateParameter(String state, CommandSender sender, MessageUtils messageUtils) {
        if (state != null && !state.equalsIgnoreCase("on") && !state.equalsIgnoreCase("off")) {
            messageUtils.sendError(sender, "状态参数必须是 'on' 或 'off'");
            return false;
        }
        return true;
    }
    
    /**
     * 验证玩家名称并获取玩家对象
     */
    public static Player validateAndGetPlayer(String playerName, CommandSender sender, MessageUtils messageUtils) {
        if (!isValidUsername(playerName)) {
            messageUtils.sendError(sender, "无效的玩家名称: " + playerName);
            return null;
        }
        
        Player target = getOnlinePlayer(playerName);
        if (target == null) {
            messageUtils.sendError(sender, "玩家 " + playerName + " 不在线");
            return null;
        }
        
        return target;
    }
    
    /**
     * 检查是否尝试操作自己
     */
    public static boolean isSelfOperation(CommandSender sender, String targetPlayer) {
        return sender.getName().equalsIgnoreCase(targetPlayer);
    }
    
    /**
     * 验证批量操作参数
     */
    public static boolean validateBatchOperation(String operation, CommandSender sender, MessageUtils messageUtils) {
        if (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && 
            !operation.equalsIgnoreCase("list") && !operation.equalsIgnoreCase("clear")) {
            messageUtils.sendError(sender, "无效的批量操作。可用操作: add, remove, list, clear");
            return false;
        }
        return true;
    }
    
    /**
     * 验证审计操作参数
     */
    public static boolean validateAuditOperation(String operation, CommandSender sender, MessageUtils messageUtils) {
        if (!operation.equalsIgnoreCase("view") && !operation.equalsIgnoreCase("clear") && 
            !operation.equalsIgnoreCase("export")) {
            messageUtils.sendError(sender, "无效的审计操作。可用操作: view, clear, export");
            return false;
        }
        return true;
    }
    
    /**
     * 检查速率限制（简单实现）
     */
    public static boolean checkRateLimit(CommandSender sender, long lastCommandTime, long cooldownMillis) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCommand = currentTime - lastCommandTime;
        
        if (timeSinceLastCommand < cooldownMillis) {
            long remaining = (cooldownMillis - timeSinceLastCommand) / 1000;
            return false;
        }
        
        return true;
    }

    /**
     * 检查速率限制（简化版本）
     */
    public static boolean checkRateLimit(CommandSender sender) {
        // 简化实现，总是返回true
        return true;
    }
    
    /**
     * 验证配置值范围
     */
    public static boolean validateConfigRange(int value, int min, int max, String configName) {
        return value >= min && value <= max;
    }
    
    /**
     * 验证配置值范围（带默认值）
     */
    public static int validateConfigRangeWithDefault(int value, int min, int max, int defaultValue, String configName) {
        if (value < min || value > max) {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * 检查是否为控制台
     */
    public static boolean isConsole(CommandSender sender) {
        return !(sender instanceof Player);
    }
    
    /**
     * 获取发送者名称（处理控制台）
     */
    public static String getSenderName(CommandSender sender) {
        return isConsole(sender) ? "CONSOLE" : sender.getName();
    }
    
    /**
     * 验证文件路径安全性
     */
    public static boolean isSafeFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // 防止路径遍历攻击
        if (path.contains("..") || path.contains("//") || path.contains("\\\\")) {
            return false;
        }
        
        // 防止绝对路径
        if (path.startsWith("/") || path.startsWith("\\") || path.contains(":")) {
            return false;
        }
        
        return true;
    }

    /**
     * 获取玩家UUID（通过名称），支持离线玩家
     */
    public static UUID getPlayerUUID(String playerName) {
        Player player = getOnlinePlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        // 支持离线玩家
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId();
        }
        
        return null;
    }

    /**
     * 验证玩家列表
     */
    public static java.util.List<UUID> validatePlayerList(java.util.List<String> playerNames) {
        java.util.List<UUID> validUUIDs = new java.util.ArrayList<>();
        for (String playerName : playerNames) {
            UUID uuid = getPlayerUUID(playerName);
            if (uuid != null) {
                validUUIDs.add(uuid);
            }
        }
        return validUUIDs;
    }

    /**
     * 解析玩家选择符 (@a, @p, @s)，支持离线玩家
     */
    public static List<UUID> parsePlayerSelector(String selector, CommandSender sender) {
        List<UUID> playerUUIDs = new ArrayList<>();
        
        switch (selector.toLowerCase()) {
            case "@a":
                // 所有在线玩家
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerUUIDs.add(player.getUniqueId());
                }
                break;
            case "@p":
                // 最近的玩家（如果是玩家执行命令，则返回自己；如果是控制台，则返回最近的玩家）
                if (sender instanceof Player) {
                    playerUUIDs.add(((Player) sender).getUniqueId());
                } else {
                    Player nearestPlayer = getNearestPlayer();
                    if (nearestPlayer != null) {
                        playerUUIDs.add(nearestPlayer.getUniqueId());
                    }
                }
                break;
            case "@s":
                // 自己（仅限玩家）
                if (sender instanceof Player) {
                    playerUUIDs.add(((Player) sender).getUniqueId());
                }
                break;
            default:
                // 普通玩家名称 - 支持离线玩家
                UUID uuid = getPlayerUUID(selector);
                if (uuid != null) {
                    playerUUIDs.add(uuid);
                }
                break;
        }
        
        return playerUUIDs;
    }

    /**
     * 获取最近的玩家（用于控制台执行 @p 选择符）
     */
    private static Player getNearestPlayer() {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 简化实现：返回第一个在线玩家
            return player;
        }
        
        return null;
    }

    /**
     * 验证权限级别参数
     */
    public static boolean validatePermissionLevel(String level, CommandSender sender, MessageUtils messageUtils) {
        if (level != null && !level.equalsIgnoreCase("player") && 
            !level.equalsIgnoreCase("lowop") && !level.equalsIgnoreCase("op")) {
            messageUtils.sendError(sender, "权限级别必须是 'player', 'lowop' 或 'op'");
            return false;
        }
        return true;
    }


    /**
     * 验证玩家名称并获取玩家UUID（支持离线玩家）
     */
    public static UUID validateAndGetPlayerUUID(String playerName, CommandSender sender, MessageUtils messageUtils) {
        if (!isValidUsername(playerName)) {
            messageUtils.sendError(sender, "无效的玩家名称: " + playerName);
            return null;
        }
        
        UUID uuid = getPlayerUUID(playerName);
        if (uuid == null) {
            messageUtils.sendError(sender, "玩家 " + playerName + " 不存在或从未登录过服务器");
            return null;
        }
        
        return uuid;
    }

    /**
     * 验证玩家列表（支持选择符）
     */
    public static List<UUID> validatePlayerListWithSelectors(List<String> playerSelectors, CommandSender sender) {
        List<UUID> validUUIDs = new ArrayList<>();
        for (String selector : playerSelectors) {
            List<UUID> uuids = parsePlayerSelector(selector, sender);
            validUUIDs.addAll(uuids);
        }
        return validUUIDs;
    }
}
