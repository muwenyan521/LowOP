package wangxianming.lowop.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import wangxianming.lowop.managers.ConfigManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab 补全器，为 /lowop 命令提供智能补全
 */
public class LowOPTabCompleter implements TabCompleter {
    
    private final ConfigManager configManager;
    
    public LowOPTabCompleter(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("lowop.manage")) {
            return completions;
        }
        
        switch (args.length) {
            case 1:
                // 第一参数：子命令或玩家名
                completions.addAll(getSubCommands(sender));
                completions.addAll(getAllPlayerNames());
                break;
                
            case 2:
                // 第二参数：根据第一参数决定
                String firstArg = args[0].toLowerCase();
                if (isSubCommand(firstArg)) {
                    // 如果是子命令，根据子命令提供补全
                    completions.addAll(getSubCommandCompletions(firstArg, sender));
                } else {
                    // 如果是玩家名，提供状态参数
                    completions.addAll(Arrays.asList("on", "off", "status"));
                }
                break;
                
            case 3:
                // 第三参数：批量操作的目标
                if ("batch".equalsIgnoreCase(args[0]) && "add".equalsIgnoreCase(args[1])) {
                    completions.addAll(getOnlinePlayerNames());
                }
                break;
                
            default:
                break;
        }
        
        // 过滤匹配当前输入的内容
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取可用的子命令列表
     */
    private List<String> getSubCommands(CommandSender sender) {
        List<String> commands = new ArrayList<>();
        
        commands.add("help");
        commands.add("reload");
        commands.add("status");
        commands.add("health");
        commands.add("audit");
        commands.add("version");
        
        if (sender.hasPermission("lowop.batch")) {
            commands.add("batch");
        }
        
        return commands;
    }
    
    /**
     * 获取在线玩家名称列表
     */
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有玩家名称列表（包括离线玩家）
     */
    private List<String> getAllPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        
        // 添加在线玩家
        playerNames.addAll(getOnlinePlayerNames());
        
        // 添加离线玩家（曾经登录过的玩家）
        for (org.bukkit.OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                String name = offlinePlayer.getName();
                if (!playerNames.contains(name)) {
                    playerNames.add(name);
                }
            }
        }
        
        return playerNames;
    }
    
    /**
     * 检查是否为子命令
     */
    private boolean isSubCommand(String arg) {
        return Arrays.asList("help", "reload", "status", "health", "audit", "batch", "version")
                .contains(arg.toLowerCase());
    }
    
    /**
     * 获取子命令的补全选项
     */
    private List<String> getSubCommandCompletions(String subCommand, CommandSender sender) {
        List<String> completions = new ArrayList<>();
        
        switch (subCommand.toLowerCase()) {
            case "batch":
                if (sender.hasPermission("lowop.batch")) {
                    completions.add("add");
                    completions.add("remove");
                    completions.add("list");
                    completions.add("clear");
                }
                break;
                
            case "audit":
                completions.add("view");
                completions.add("clear");
                completions.add("export");
                break;
                
            case "status":
                completions.addAll(getAllPlayerNames());
                break;
                
            default:
                break;
        }
        
        return completions;
    }
}
