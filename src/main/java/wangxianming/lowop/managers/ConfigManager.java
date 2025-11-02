package wangxianming.lowop.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import wangxianming.lowop.LowOP;

import java.io.File;
import java.io.IOException;
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
    public String getAdminGroup() {
        return config.getString("permission-groups.admin", "otherop");
    }

    public String getDefaultGroup() {
        return config.getString("permission-groups.default", "default");
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
}
