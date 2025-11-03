package wangxianming.lowop.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import wangxianming.lowop.LowOP;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final LowOP plugin;
    private FileConfiguration config;
    private File configFile;
    
    private FileConfiguration messages;
    private File messagesFile;

    public ConfigManager(LowOP plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // Load main config
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Load messages config
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Validate configuration
        validateConfig();
        
        plugin.getLogger().info("Configuration loaded successfully");
    }

    private void validateConfig() {
        // Set defaults if missing
        if (!config.contains("permission-groups.admin")) {
            config.set("permission-groups.admin", "otherop");
        }
        if (!config.contains("permission-groups.default")) {
            config.set("permission-groups.default", "default");
        }
        if (!config.contains("settings.audit-log-enabled")) {
            config.set("settings.audit-log-enabled", true);
        }
        if (!config.contains("settings.auto-save-interval")) {
            config.set("settings.auto-save-interval", 300); // 5 minutes
        }
        if (!config.contains("settings.rate-limit")) {
            config.set("settings.rate-limit", 3); // commands per second
        }
        
        saveConfig();
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        validateConfig();
        plugin.getLogger().info("Configuration reloaded successfully");
    }

    private void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
        }
    }

    // Getters for config values
    public String getPlayerGroup() {
        return config.getString("permission-groups.player-group", "default");
    }

    public String getLowOPGroup() {
        return config.getString("permission-groups.lowop-group", "otherop");
    }

    public String getOPGroup() {
        return config.getString("permission-groups.op-group", "op");
    }

    public boolean isAuditLogEnabled() {
        return config.getBoolean("settings.audit-log-enabled", true);
    }

    public int getAutoSaveInterval() {
        return config.getInt("settings.auto-save-interval", 300);
    }

    public int getRateLimit() {
        return config.getInt("settings.rate-limit", 3);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    // Message getters
    public String getMessage(String path, String defaultValue) {
        return messages.getString(path, defaultValue).replace('&', '§');
    }

    public List<String> getMessageList(String path, List<String> defaultValue) {
        List<String> list = messages.getStringList(path);
        if (list.isEmpty()) {
            return defaultValue;
        }
        list.replaceAll(line -> line.replace('&', '§'));
        return list;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    // Add missing method
    public List<String> getHelpMessage() {
        return getMessageList("messages.help", Arrays.asList(
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
    }
}
