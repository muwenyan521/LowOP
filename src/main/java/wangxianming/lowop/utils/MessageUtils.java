package wangxianming.lowop.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wangxianming.lowop.managers.ConfigManager;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息工具类，提供统一的消息发送和格式化功能
 */
public class MessageUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final ConfigManager configManager;
    
    public MessageUtils(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * 发送消息给指定发送者
     */
    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(colorize(message));
        }
    }
    
    /**
     * 发送消息给指定发送者（无占位符）
     */
    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }
    
    /**
     * 发送消息给玩家
     */
    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(colorize(message));
        }
    }
    
    /**
     * 发送消息给玩家（无占位符）
     */
    public void sendMessage(Player player, String key) {
        sendMessage(player, key, null);
    }
    
    /**
     * 发送成功消息
     */
    public void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(colorize("&a✓ &7" + message));
    }
    
    /**
     * 发送错误消息
     */
    public void sendError(CommandSender sender, String message) {
        sender.sendMessage(colorize("&c✗ &7" + message));
    }
    
    /**
     * 发送警告消息
     */
    public void sendWarning(CommandSender sender, String message) {
        sender.sendMessage(colorize("&e⚠ &7" + message));
    }
    
    /**
     * 发送信息消息
     */
    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(colorize("&bℹ &7" + message));
    }
    
    /**
     * 发送权限管理状态消息
     */
    public void sendPermissionStatus(Player player, boolean enabled) {
        if (enabled) {
            sendMessage(player, "permission-enabled");
        } else {
            sendMessage(player, "permission-disabled");
        }
    }
    
    /**
     * 发送批量操作结果
     */
    public void sendBatchResult(CommandSender sender, int successCount, int totalCount, String operation) {
        String message = getMessage("batch-result", Map.of(
            "success", String.valueOf(successCount),
            "total", String.valueOf(totalCount),
            "operation", operation
        ));
        sender.sendMessage(colorize(message));
    }
    
    /**
     * 发送审计日志条目
     */
    public void sendAuditEntry(CommandSender sender, String entry) {
        sender.sendMessage(colorize("&8[&6审计&8] &7" + entry));
    }
    
    /**
     * 获取格式化后的消息
     */
    private String getMessage(String key, Map<String, String> placeholders) {
        String message = configManager.getMessage(key, "&c消息键 '" + key + "' 未找到");
        if (message == null || message.isEmpty()) {
            return "&c消息键 '" + key + "' 未找到";
        }
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * 颜色代码转换（支持十六进制颜色）
     */
    public static String colorize(String message) {
        if (message == null) return "";
        
        // 转换十六进制颜色代码
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + hex.charAt(0) + "§" + hex.charAt(1) + 
                "§" + hex.charAt(2) + "§" + hex.charAt(3) + "§" + hex.charAt(4) + "§" + hex.charAt(5));
        }
        matcher.appendTail(buffer);
        
        // 转换传统颜色代码
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * 发送多行消息
     */
    public void sendMultilineMessage(CommandSender sender, List<String> lines) {
        for (String line : lines) {
            sender.sendMessage(colorize(line));
        }
    }
    
    /**
     * 发送帮助消息
     */
    public void sendHelpMessage(CommandSender sender) {
        List<String> helpLines = configManager.getHelpMessage();
        sendMultilineMessage(sender, helpLines);
    }
    
    /**
     * 发送版本信息
     */
    public void sendVersionInfo(CommandSender sender, String version, String author) {
        String message = getMessage("version-info", Map.of(
            "version", version,
            "author", author
        ));
        sender.sendMessage(colorize(message));
    }
}
