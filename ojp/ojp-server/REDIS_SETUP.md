# Redis 设置指南

## ⚠️ 重要说明

**Redis 是 OJP Server 的核心依赖**，负责以下关键功能：
- 缓存规则配置存储
- SQL 执行统计信息
- 表信息统计缓存
- 分布式缓存管理

应用程序启动时会自动尝试连接 Redis，如果连接失败，应用程序仍会启动但相关功能将不可用。

## 🚀 快速开始

### 1. 安装 Redis

#### Windows 方式 1：使用 Chocolatey
```bash
choco install redis-64
```

#### Windows 方式 2：使用 WSL
```bash
# 在 WSL 中安装
sudo apt update
sudo apt install redis-server
sudo systemctl start redis-server
```

#### Windows 方式 3：手动安装
1. 下载 Redis for Windows：https://github.com/microsoftarchive/redis/releases
2. 解压到 `C:\Redis`
3. 运行 `redis-server.exe`

### 2. 启动 Redis 服务

#### 方式 1：命令行启动
```bash
redis-server
```

#### 方式 2：Windows 服务
```bash
# 安装为 Windows 服务
redis-server --service-install redis.windows.conf
# 启动服务
redis-server --service-start
# 停止服务
redis-server --service-stop
```

### 3. 测试连接

```bash
# 测试 Redis 是否运行
redis-cli ping
# 应该返回 PONG

# 或者使用 telnet
telnet localhost 6379
```

### 4. 启用 OJP Server 的 Redis 功能

编辑 `application.yml`：

```yaml
ojp:
  redis:
    enabled: true  # 改为 true 启用 Redis
    host: localhost
    port: 6379
    password:  # 如果有密码，在这里设置
    database: 0
    timeout: 2000
```

## 🔧 配置选项

### Redis 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `enabled` | `true` | Redis 是核心依赖，默认启用 |
| `host` | `localhost` | Redis 服务器地址 |
| `port` | `6379` | Redis 端口 |
| `password` | 空 | Redis 密码（如果有） |
| `database` | `0` | 数据库索引 |
| `timeout` | `2000` | 连接超时时间（毫秒） |
| `ssl` | `false` | 是否启用 SSL |

### 连接池配置

```yaml
ojp:
  redis:
    pool:
      max-active: 8    # 最大活跃连接数
      max-idle: 8      # 最大空闲连接数
      min-idle: 0      # 最小空闲连接数
      max-wait: -1     # 最大等待时间（-1表示无限制）
```

## 🚨 故障排除

### 常见问题

#### 1. 连接被拒绝
```
Caused by: io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**解决方案：**
- 检查 Redis 服务是否运行：`netstat -an | findstr :6379`
- 检查防火墙设置
- 确认 Redis 配置中的端口号

#### 2. 认证失败
```
Caused by: io.lettuce.core.RedisException: NOAUTH Authentication required
```

**解决方案：**
- 在 `application.yml` 中设置正确的密码
- 或者禁用 Redis 密码认证

#### 3. 超时错误
```
Caused by: io.lettuce.core.RedisException: Command timed out
```

**解决方案：**
- 增加 `timeout` 值
- 检查网络延迟
- 检查 Redis 服务器性能

### 应用程序启动行为

- **Redis 可用时**：应用程序正常启动，所有功能可用
- **Redis 不可用时**：应用程序仍会启动，但会记录警告日志
- **自动重连**：应用程序会定期尝试重新连接 Redis
- **功能降级**：Redis 不可用时，相关功能会抛出异常，但不会导致应用崩溃

**注意**：由于 Redis 是核心依赖，不建议禁用。如果遇到连接问题，请检查 Redis 服务状态。

## 📊 监控和日志

### 启用详细日志

```yaml
logging:
  level:
    org.openjdbcproxy.grpc.server.config: DEBUG
    org.openjdbcproxy.grpc.server.smartcache: DEBUG
```

### 健康检查

Redis 连接状态会记录在日志中：

```
INFO  - Redis连接成功: PONG
WARN  - Redis连接检查失败: Connection refused
INFO  - 尝试重新连接Redis...
INFO  - Redis重连失败: Connection refused
```

### 状态监控

可以通过 `RedisConnectionManager.getStatus()` 方法获取 Redis 连接状态：

```java
@Autowired
private RedisConnectionManager redisManager;

public void checkRedisStatus() {
    RedisConnectionManager.RedisStatus status = redisManager.getStatus();
    if (status.isConnected()) {
        log.info("Redis连接正常: {}:{}", status.getHost(), status.getPort());
    } else {
        log.warn("Redis连接异常: {}:{}", status.getHost(), status.getPort());
    }
}
```

## 🔒 安全建议

1. **设置强密码**：在生产环境中设置复杂的 Redis 密码
2. **网络隔离**：将 Redis 服务器放在内网中
3. **SSL 加密**：在公网环境中启用 SSL
4. **访问控制**：使用 Redis ACL 限制访问权限

## 🚀 快速启动脚本

### Windows 批处理脚本
```bash
# 以管理员身份运行
start-redis.bat
```

### PowerShell 脚本
```powershell
# 以管理员身份运行
.\start-redis.ps1
```

## 🔍 健康检查 API

### 基本健康状态
```bash
GET /api/health/redis
```

### 详细状态信息
```bash
GET /api/health/redis/details
```

响应示例：
```json
{
  "status": "UP",
  "connected": true,
  "host": "localhost",
  "port": 6379,
  "connectionOpen": true,
  "ping": "PONG",
  "pingSuccess": true,
  "timestamp": 1703123456789
}
```

## 📚 相关文档

- [Redis 官方文档](https://redis.io/documentation)
- [Spring Boot Redis 集成](https://spring.io/projects/spring-data-redis)
- [Lettuce 客户端文档](https://lettuce.io/)
