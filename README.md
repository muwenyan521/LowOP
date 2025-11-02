# LowOP - Minecraft 权限管理插件

![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/muwenyan521/LowOP/build.yml)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/muwenyan521/LowOP)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21-blue)
![Spigot API](https://img.shields.io/badge/Spigot-1.21--R0.1--SNAPSHOT-orange)

一个功能强大的 Minecraft Spigot 插件，用于动态管理玩家管理员权限状态，支持批量操作、审计日志和健康监控。

## ✨ 特性

### 核心功能
- **动态权限管理**: 通过命令快速切换玩家管理员权限状态
- **状态持久化**: 使用 YAML 文件保存玩家权限状态
- **批量操作**: 支持批量添加/移除管理员权限
- **智能恢复**: 玩家登录时自动恢复权限状态

### 高级功能
- **审计日志**: 完整记录所有权限变更操作
- **健康监控**: 实时监控系统状态和性能
- **Tab 补全**: 智能命令补全支持
- **多语言消息**: 可配置的消息系统
- **权限验证**: 严格的权限检查和输入验证

### 生产就绪
- **异步操作**: 避免阻塞主线程
- **错误处理**: 完善的异常处理机制
- **配置热重载**: 无需重启即可重载配置
- **安全防护**: 防止权限滥用和误操作

## 🚀 快速开始

### 安装要求
- Minecraft 服务器 1.21+
- Spigot/Paper 1.21-R0.1-SNAPSHOT
- LuckPerms 5.4+
- Java 17+

### 安装步骤

1. **下载插件**
   ```bash
   # 从 Releases 页面下载最新版本
   # 或使用 GitHub Actions 构建
   ```

2. **安装到服务器**
   - 将 `LowOP.jar` 放入服务器的 `plugins` 目录
   - 重启服务器

3. **配置权限组**
   - 确保 LuckPerms 中已创建以下权限组：
     - `otherop` (管理员权限组)
     - `default` (默认权限组)

4. **权限设置**
   ```yaml
   # 在 LuckPerms 中设置默认权限
   /lp creategroup default
   /lp creategroup otherop
   /lp group default parent add default
   /lp group otherop parent add otherop
   ```

## 📖 使用指南

### 基础命令

```bash
# 切换玩家权限状态
/lowop <玩家名> [on/off]

# 示例
/lowop Steve on        # 启用 Steve 的管理员权限
/lowop Alex off        # 禁用 Alex 的管理员权限  
/lowop Bob             # 切换 Bob 的权限状态
```

### 高级命令

```bash
# 查看帮助
/lowop help

# 重载配置
/lowop reload

# 查看权限状态
/lowop status [玩家名]

# 系统健康检查
/lowop health

# 审计日志管理
/lowop audit view      # 查看审计日志
/lowop audit clear     # 清空审计日志
/lowop audit export    # 导出审计日志

# 批量操作
/lowop batch add <玩家名>    # 添加到批量列表
/lowop batch remove <玩家名> # 从批量列表移除
/lowop batch list           # 查看批量列表
/lowop batch clear          # 清空批量列表

# 版本信息
/lowop version
```

### 权限节点

| 权限节点 | 描述 | 默认 |
|---------|------|------|
| `lowop.manage` | 基础管理权限 | OP |
| `lowop.batch` | 批量操作权限 | OP |
| `lowop.audit` | 审计日志权限 | OP |
| `lowop.reload` | 重载配置权限 | OP |

## ⚙️ 配置说明

### 主要配置文件

#### config.yml
```yaml
# 插件设置
settings:
  enabled: true
  debug: false
  auto-save-interval: 300
  command-cooldown: 3

# 权限组配置  
permission-groups:
  admin-group: "otherop"
  default-group: "default"
  clear-existing: true

# 审计日志
audit:
  enabled: true
  log-file: "audit.log"
  retention-days: 30

# 健康检查
health:
  enabled: true
  check-interval: 60
```

#### messages.yml
```yaml
# 权限消息
permission-enabled: "&a你已成为服务器管理员。"
permission-disabled: "&c你的管理员权限已被移除。"

# 错误消息  
no-permission: "&c你没有权限执行此操作。"
player-offline: "&c玩家 &6{player} &c不在线。"

# 批量操作消息
batch-result: "&a批量操作完成: &6{success}&a/&6{total} &a成功"
```

## 🔧 开发指南

### 构建环境
- JDK 17+
- Maven 3.6+
- Spigot API 1.21-R0.1-SNAPSHOT

### 构建步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/muwenyan521/LowOP.git
   cd LowOP
   ```

2. **编译项目**
   ```bash
   mvn clean compile
   ```

3. **打包插件**
   ```bash
   mvn package
   ```

4. **运行测试**
   ```bash
   mvn test
   ```

### 项目结构
```
src/
├── main/
│   ├── java/
│   │   └── wangxianming/
│   │       └── lowop/
│   │           ├── LowOP.java              # 主插件类
│   │           ├── commands/               # 命令处理器
│   │           │   ├── LowOPCommand.java
│   │           │   └── LowOPTabCompleter.java
│   │           ├── listeners/              # 事件监听器
│   │           │   └── PlayerJoinListener.java
│   │           ├── managers/               # 管理器类
│   │           │   ├── ConfigManager.java
│   │           │   ├── StateManager.java
│   │           │   ├── PermissionManager.java
│   │           │   ├── AuditManager.java
│   │           │   └── HealthManager.java
│   │           └── utils/                  # 工具类
│   │               ├── MessageUtils.java
│   │               └── ValidationUtils.java
│   └── resources/
│       ├── plugin.yml                      # 插件描述文件
│       ├── config.yml                      # 主配置文件
│       └── messages.yml                    # 消息配置文件
```

## 🛠️ CI/CD 流程

项目使用 GitHub Actions 实现自动化构建和部署：

- **自动构建**: 在 push 和 pull request 时自动构建
- **质量检查**: 代码风格检查、PMD 分析、测试覆盖率
- **安全扫描**: OWASP 依赖安全检查
- **自动发布**: 创建 release 时自动打包发布

## 📊 监控和日志

### 审计日志
插件记录所有重要操作到审计日志：
- 权限变更操作
- 玩家登录/退出
- 命令执行
- 系统错误

### 健康监控
实时监控系统状态：
- 内存使用率
- 磁盘空间
- 数据库连接状态
- 审计日志状态

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🐛 问题反馈

如果您遇到任何问题，请通过以下方式反馈：

1. 查看 [Issues](https://github.com/muwenyan521/LowOP/issues) 页面
2. 创建新的 Issue，包含：
   - Minecraft 服务器版本
   - 插件版本
   - 错误日志
   - 复现步骤

## 🔗 相关链接

- [SpigotMC](https://www.spigotmc.org/)
- [LuckPerms](https://luckperms.net/)
- [Maven](https://maven.apache.org/)

---

**LowOP** - 让权限管理更简单、更安全！ 🎮
