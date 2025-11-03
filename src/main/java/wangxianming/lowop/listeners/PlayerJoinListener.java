package wangxianming.lowop.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import wangxianming.lowop.managers.StateManager;
import wangxianming.lowop.managers.PermissionManager;
import wangxianming.lowop.managers.AuditManager;
import wangxianming.lowop.utils.MessageUtils;

import java.util.concurrent.CompletableFuture;

/**
 * 玩家加入事件监听器，处理玩家登录时的权限状态恢复
 * 支持三级权限系统：PLAYER, LOWOP, OP
 */
public class PlayerJoinListener implements Listener {
    
    private final StateManager stateManager;
    private final PermissionManager permissionManager;
    private final AuditManager auditManager;
    private final MessageUtils messageUtils;
    
    public PlayerJoinListener(StateManager stateManager, PermissionManager permissionManager, 
                            AuditManager auditManager, MessageUtils messageUtils) {
        this.stateManager = stateManager;
        this.permissionManager = permissionManager;
        this.auditManager = auditManager;
        this.messageUtils = messageUtils;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 异步处理权限状态恢复和检测
        CompletableFuture.runAsync(() -> {
            try {
                handlePlayerJoin(player);
            } catch (Exception e) {
                auditManager.logError("PlayerJoinListener", "onPlayerJoin", "处理玩家 " + player.getName() + " 登录时发生错误: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理玩家登录逻辑
     */
    private void handlePlayerJoin(Player player) {
        // 检测玩家当前的权限级别
        PermissionManager.PermissionLevel currentLevel = permissionManager.detectPlayerPermissionLevel(player.getName()).join();
        PermissionManager.PermissionLevel storedLevel = stateManager.getPlayerPermissionLevel(player.getUniqueId());
        
        // 如果存储的级别与检测到的级别不一致，更新存储状态
        if (storedLevel != currentLevel) {
            stateManager.setPlayerPermissionLevel(player.getUniqueId(), currentLevel, "System");
            auditManager.logPermissionLevelChange(player.getUniqueId(), storedLevel, currentLevel, "System");
        }
        
        // 确保权限状态正确
        ensureCorrectPermissions(player, currentLevel);
        
        // 记录登录审计
        auditManager.logPlayerJoinWithPermissionLevel(player.getName(), player.getUniqueId(), currentLevel);
    }
    
    /**
     * 确保权限状态正确
     */
    private void ensureCorrectPermissions(Player player, PermissionManager.PermissionLevel currentLevel) {
        // 检查当前权限状态是否正确
        boolean hasCorrectPermissions = permissionManager.checkPermissionLevelState(player.getName(), currentLevel);
        
        if (!hasCorrectPermissions) {
            // 权限状态不正确，需要修复
            auditManager.logPermissionFix(player.getName(), "登录时修复权限状态为: " + currentLevel);
            
            // 执行权限修复 - 异步处理
            permissionManager.setPlayerPermissionLevel(player.getUniqueId(), currentLevel, null).thenAccept(success -> {
                if (success) {
                    switch (currentLevel) {
                        case OP:
                            messageUtils.sendMessage(player, "permission-restored");
                            break;
                        case LOWOP:
                            messageUtils.sendMessage(player, "lowop-welcome");
                            break;
                        case PLAYER:
                            // 普通玩家不需要特殊消息
                            break;
                    }
                    auditManager.logPermissionRestore(player.getName(), "登录时自动恢复权限级别: " + currentLevel);
                } else {
                    messageUtils.sendError(player, "权限恢复失败，请联系管理员");
                    auditManager.logError("PlayerJoinListener", "ensureCorrectPermissions", "玩家 " + player.getName() + " 权限级别 " + currentLevel + " 恢复失败");
                }
            });
        } else {
            // 权限状态正确，发送相应的欢迎消息
            switch (currentLevel) {
                case OP:
                    messageUtils.sendMessage(player, "admin-welcome");
                    break;
                case LOWOP:
                    messageUtils.sendMessage(player, "lowop-welcome");
                    break;
                case PLAYER:
                    // 普通玩家不需要特殊欢迎消息
                    break;
            }
        }
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 记录退出审计
        PermissionManager.PermissionLevel level = stateManager.getPlayerPermissionLevel(player.getUniqueId());
        auditManager.logPlayerQuitWithPermissionLevel(player.getName(), player.getUniqueId(), level);
        
        // 可以在这里执行退出时的清理操作
        // 例如：保存最终状态、清理临时数据等
    }
    
    /**
     * 处理玩家踢出事件
     */
    @EventHandler
    public void onPlayerKick(org.bukkit.event.player.PlayerKickEvent event) {
        Player player = event.getPlayer();
        
        // 记录踢出审计
        PermissionManager.PermissionLevel level = stateManager.getPlayerPermissionLevel(player.getUniqueId());
        auditManager.logPlayerKickWithPermissionLevel(player.getName(), player.getUniqueId(), level, event.getReason());
    }
}
