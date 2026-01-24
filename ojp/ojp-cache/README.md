# OJP Cache Module

OJP 智能缓存模块，提供灵活高效的数据库查询缓存功能。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [缓存规则](#缓存规则)
- [配置说明](#配置说明)
- [API 文档](#api-文档)
- [最佳实践](#最佳实践)

## ✨ 功能特性

### 核心功能

- **表同步状态决策**
  - 基于表级同步状态的缓存决策
  - 自动解析 SQL 中的表名
  - 检查表同步就绪状态决定查询路由

- **智能缓存管理**
  - 慢查询统计和分析
  - 性能指标收集
  - 缓存命中率监控

- **CDC 感知**
  - 与 SeaTunnel CDC 集成
  - 实时感知数据变更
  - 表同步状态监控

## 🏗️ 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                     ojp-cache                            │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │  Rule Engine │ Cache Store  │  Statistics Engine  │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                      Redis                               │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │  Rule Config │  Query Cache │  Statistics Data   │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 缓存决策流程

1. **查询拦截**: 拦截 JDBC 查询请求
2. **SQL 解析**: 解析查询中的表名
3. **同步状态检查**: 检查涉及表的 CDC 同步状态
4. **路由决策**: 根据同步状态决定查询 StarRocks（缓存）还是 MySQL（源库）
5. **统计更新**: 记录查询性能和决策结果

## 🚀 快速开始

### 环境要求

- **Java**: 11+
- **Redis**: 6.0+
- **Spring Boot**: 3.3.3+

### 配置 Redis

在 `application.yml` 中配置 Redis 连接：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ""
      database: 0
      timeout: 5000ms

fluxCache:
  redis:
    host: "localhost"
    port: 6379
    database: 0
    password: ""
    timeout: 5000
```

### 启动服务

```bash
# 构建
mvn clean install -DskipTests

# 运行
mvn spring-boot:run
```

## 📝 缓存决策机制

### 表同步状态决策

缓存决策基于表的 CDC 同步状态：

- **同步就绪**: 表数据已通过 SeaTunnel 同步到 StarRocks，可以走缓存查询
- **同步未就绪**: 表数据同步尚未完成，回源查询 MySQL

```java
// 决策逻辑示例
public boolean makeDecision(String connHash, String sql) {
    Set<String> tables = JSqlParserUtil.extractTableNames(sql);
    for (String table : tables) {
        if (!tableSyncStateManager.isTableReady(connHash, table)) {
            return false; // 回源查询
        }
    }
    return true; // 走缓存查询
}
```

### 慢查询管理

系统提供慢查询统计和分析功能：

- **慢查询识别**: 根据执行时间阈值识别慢查询
- **查询统计**: 记录查询频率、平均响应时间等指标
- **性能监控**: 提供查询性能分析和优化建议

```json
// 慢查询统计示例
{
  "queryId": "slow_user_query",
  "sql": "SELECT * FROM users WHERE last_login > ?",
  "executionTime": 2500,
  "frequency": 150,
  "avgTime": 2200
}
```

## ⚙️ 配置说明

### 完整配置示例

```yaml
fluxCache:
  # Redis 配置
  redis:
    host: "localhost"
    port: 6379
    database: 0
    password: ""
    username: ""
    timeout: 5000
    ssl: false

  # 数据库配置
  database:
    name: "ecommerce_db"

  # 缓存配置
  cache:
    defaultTtl: 3600           # 默认缓存时间（秒）
    maxCacheSize: 10000        # 最大缓存条目数
    enableStatistics: true     # 启用统计
    enableSlowQuery: true      # 启用慢查询识别
    slowQueryThreshold: 1000   # 慢查询阈值（毫秒）

  # CDC 配置
  cdc:
    enabled: true              # 启用 CDC 感知
    heartbeatInterval: 5000    # 心跳间隔（毫秒）
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OJP_REDIS_HOST` | Redis 主机 | `localhost` |
| `OJP_REDIS_PORT` | Redis 端口 | `6379` |
| `OJP_REDIS_PASSWORD` | Redis 密码 | `""` |
| `OJP_CACHE_DEFAULT_TTL` | 默认缓存时间 | `3600` |
| `OJP_SLOW_QUERY_THRESHOLD` | 慢查询阈值 | `1000` |

## 📚 API 文档

详细的 API 文档请参考 [RESTful_API文档.md](RESTful_API文档.md)。

### 主要 API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/cache/rules/list` | GET | 获取缓存规则列表 |
| `/api/cache/rules` | POST | 创建缓存规则 |
| `/api/cache/rules/{ruleId}` | PUT | 更新缓存规则 |
| `/api/cache/rules/{ruleId}` | DELETE | 删除缓存规则 |
| `/api/cache/queries/list` | GET | 获取慢查询列表 |
| `/api/cache/stats/tables/list` | GET | 获取表格统计 |

### API 示例

#### 创建缓存规则

```bash
curl -X POST http://localhost:8010/api/cache/rules \
  -H "Content-Type: application/json" \
  -d '{
    "ruleName": "用户基础信息缓存",
    "ruleType": "TABLES",
    "tables": ["users"],
    "ttl": 1800,
    "priority": 1,
    "enabled": true,
    "description": "缓存用户基础信息30分钟"
  }'
```

#### 获取慢查询列表

```bash
curl http://localhost:8010/api/cache/queries/list
```

#### 获取表格统计

```bash
curl http://localhost:8010/api/cache/stats/tables/list
```

## 💡 最佳实践

### 1. CDC 集成配置

- **监控配置**: 确保 SeaTunnel CDC 任务正常运行
- **状态监控**: 实时监控表同步状态变化
- **异常处理**: 及时处理同步失败或延迟的情况

### 2. 慢查询优化

- 定期查看慢查询统计
- 分析查询性能瓶颈
- 优化查询语句和索引

### 3. 性能监控

- 监控缓存决策延迟
- 跟踪查询路由统计
- 分析 StarRocks vs MySQL 查询性能对比

### 4. 数据一致性

- 确保 CDC 同步延迟在可接受范围内
- 监控表同步状态准确性
- 处理同步失败时的降级策略

### 5. 安全考虑

- 保护 Redis 访问凭据
- 限制管理 API 访问权限
- 定期审计查询统计数据

## 🔧 故障排查

### 常见问题

#### 1. 查询路由异常

**可能原因**:
- 表同步状态不准确
- SQL 解析失败
- CDC 连接异常

**解决方法**:
- 检查表同步状态管理器
- 验证 SQL 解析逻辑
- 确认 CDC 监控正常运行

#### 2. 性能下降

**可能原因**:
- 表同步延迟过高
- 查询统计收集影响性能
- Redis 连接延迟

**解决方法**:
- 优化 CDC 同步配置
- 调整统计收集频率
- 监控 Redis 响应时间

#### 3. 数据不一致

**可能原因**:
- CDC 同步中断
- 表状态更新延迟
- 并发查询时序问题

**解决方法**:
- 检查 CDC 任务状态
- 增加状态更新频率
- 实现查询排队机制

## 📊 监控指标

### 关键指标

- **缓存命中率**: 缓存命中次数 / 总查询次数
- **平均查询时间**: 查询平均响应时间
- **慢查询数量**: 超过阈值的查询数量
- **缓存条目数**: 当前缓存的查询数量
- **Redis 内存使用**: Redis 内存占用情况

### 监控端点

```bash
# Spring Boot Actuator
curl http://localhost:8010/actuator/metrics/cache.hit.rate
curl http://localhost:8010/actuator/metrics/cache.size
```

## 🤝 贡献指南

欢迎贡献代码和改进建议！

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。

---

**OJP Cache** - 让数据库查询更快速、更高效！