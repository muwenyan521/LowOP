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
        
        // 异步处理权限状态恢复
        CompletableFuture.runAsync(() -> {
            try {
                handlePlayerJoin(player);
            } catch (Exception e) {
                auditManager.logError("处理玩家 " + player.getName() + " 登录时发生错误: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理玩家登录逻辑
     */
    private void handlePlayerJoin(Player player) {
        boolean hasAdminState = stateManager.hasAdminState(player.getUniqueId());
        
        if (hasAdminState) {
            // 玩家应该有管理员权限，检查并恢复
            restoreAdminPermissions(player);
        } else {
            // 确保玩家没有管理员权限
            ensureDefaultPermissions(player);
        }
        
        // 记录登录审计
        auditManager.logPlayerJoin(player.getName(), player.getUniqueId(), hasAdminState);
    }
    
    /**
     * 恢复管理员权限
     */
    private void restoreAdminPermissions(Player player) {
        // 检查当前权限状态
        boolean hasCorrectPermissions = permissionManager.checkPermissionState(player.getName(), true);
        
        if (!hasCorrectPermissions) {
            // 权限状态不正确，需要修复
            auditManager.logPermissionFix(player.getName(), "登录时修复管理员权限");
            
            // 执行权限修复
            boolean success = permissionManager.setAdminPermissions(player.getName());
            
            if (success) {
                messageUtils.sendMessage(player, "permission-restored");
                auditManager.logPermissionRestore(player.getName(), "登录时自动恢复");
            } else {
                messageUtils.sendError(player, "管理员权限恢复失败，请联系管理员");
                auditManager.logError("玩家 " + player.getName() + " 管理员权限恢复失败");
            }
        } else {
            // 权限状态正确，发送欢迎消息
            messageUtils.sendMessage(player, "admin-welcome");
        }
    }
    
    /**
     * 确保玩家只有默认权限
     */
    private void ensureDefaultPermissions(Player player) {
        // 检查当前权限状态
        boolean hasCorrectPermissions = permissionManager.checkPermissionState(player.getName(), false);
        
        if (!hasCorrectPermissions) {
            // 权限状态不正确，需要修复
            auditManager.logPermissionFix(player.getName(), "登录时修复默认权限");
            
            // 执行权限修复
            boolean success = permissionManager.setDefaultPermissions(player.getName());
            
            if (success) {
                auditManager.logPermissionRestore(player.getName(), "登录时自动恢复默认权限");
            } else {
                auditManager.logError("玩家 " + player.getName() + " 默认权限恢复失败");
            }
        }
    }
    
    /**
     * 处理玩家退出事件（如果需要）
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 记录退出审计
        boolean hasAdminState = stateManager.hasAdminState(player.getUniqueId());
        auditManager.logPlayerQuit(player.getName(), player.getUniqueId(), hasAdminState);
        
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
        boolean hasAdminState = stateManager.hasAdminState(player.getUniqueId());
        auditManager.logPlayerKick(player.getName(), player.getUniqueId(), hasAdminState, event.getReason());
    }
}
