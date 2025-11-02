package wangxianming.lowop;

import org.bukkit.plugin.java.JavaPlugin;
import wangxianming.lowop.commands.LowOPCommand;
import wangxianming.lowop.commands.LowOPTabCompleter;
import wangxianming.lowop.listeners.PlayerJoinListener;
import wangxianming.lowop.managers.*;
import wangxianming.lowop.utils.MessageUtils;
import wangxianming.lowop.utils.ValidationUtils;

import java.util.logging.Level;

public class LowOP extends JavaPlugin {

    private static LowOP instance;
    
    private ConfigManager configManager;
    private StateManager stateManager;
    private PermissionManager permissionManager;
    private AuditManager auditManager;
    private HealthManager healthManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        initializeManagers();
        
        // Register commands and completers
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Perform health check
        performStartupHealthCheck();
        
        getLogger().info("LowOP has been enabled successfully!");
        getLogger().info("Version: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Save all data before shutdown
        if (stateManager != null) {
            stateManager.saveAllData();
        }
        
        if (auditManager != null) {
            auditManager.flushLogs();
        }
        
        getLogger().info("LowOP has been disabled successfully!");
    }

    private void initializeManagers() {
        try {
            configManager = new ConfigManager(this);
            stateManager = new StateManager(this);
            permissionManager = new PermissionManager(this);
            auditManager = new AuditManager(this);
            healthManager = new HealthManager(this);
            
            getLogger().info("All managers initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize managers", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerCommands() {
        try {
            LowOPCommand lowopCommand = new LowOPCommand(this);
            getCommand("lowop").setExecutor(lowopCommand);
            getCommand("lowop").setTabCompleter(new LowOPTabCompleter(configManager));
            
            getLogger().info("Commands registered successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register commands", e);
        }
    }

    private void registerListeners() {
        try {
            MessageUtils messageUtils = new MessageUtils(configManager);
            getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(stateManager, permissionManager, auditManager, messageUtils), 
                this
            );
            getLogger().info("Listeners registered successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register listeners", e);
        }
    }

    private void performStartupHealthCheck() {
        try {
            boolean healthy = healthManager.performHealthCheck();
            if (!healthy) {
                getLogger().warning("Health check reported some issues. Check logs for details.");
            } else {
                getLogger().info("Startup health check passed");
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Startup health check failed", e);
        }
    }

    // Getters for managers
    public static LowOP getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public AuditManager getAuditManager() {
        return auditManager;
    }

    public HealthManager getHealthManager() {
        return healthManager;
    }
}
