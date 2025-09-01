# OJP Server 智能缓存拦截器使用指南

## 🚀 快速开始

### 1. 环境准备

确保您的环境中已经配置了以下组件：

- **Redis 服务器**：用于存储缓存规则和缓存数据
- **OJP Server**：已集成智能缓存拦截器
- **Prometheus**：用于指标监控（可选）
- **Grafana**：用于指标可视化（可选）

### 2. 配置 Redis 连接

在 `application.yml` 中配置 Redis 连接信息：

```yaml
ojp:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    database: ${REDIS_DATABASE:0}
    password: ${REDIS_PASSWORD:}
    username: ${REDIS_USERNAME:}
    timeout: ${REDIS_TIMEOUT:10000}
```

### 3. 启动服务

启动 OJP Server，智能缓存拦截器将自动生效：

```bash
java -jar ojp-server.jar
```

## 📋 缓存规则配置

### 规则格式说明

缓存规则采用 JSON 格式，包含以下字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 规则名称，唯一标识 |
| `description` | String | 否 | 规则描述 |
| `type` | String | 是 | 规则类型（TABLE_NAME/REGEX/QUERY_TYPE/GLOBAL） |
| `tables` | Array | 否 | 表名列表（TABLE_NAME 类型） |
| `tablesAny` | Array | 否 | 任意表名列表（TABLE_NAME 类型） |
| `tablesAll` | Array | 否 | 所有表名列表（TABLE_NAME 类型） |
| `regex` | String | 否 | 正则表达式（REGEX 类型） |
| `pattern` | String | 否 | 查询类型模式（QUERY_TYPE 类型） |
| `ttl` | String | 是 | 缓存生存时间（ISO 8601 格式） |
| `priority` | Integer | 是 | 规则优先级（数字越大优先级越高） |
| `enabled` | Boolean | 是 | 是否启用规则 |

### 规则类型详解

#### 1. 表名规则 (TABLE_NAME)

基于查询中涉及的表名进行匹配。

```json
{
  "name": "users_table_cache",
  "description": "缓存用户表查询",
  "type": "TABLE_NAME",
  "tables": ["users"],
  "ttl": "PT5M",
  "priority": 10,
  "enabled": true
}
```

**匹配示例**：
- ✅ `SELECT * FROM users WHERE id = 1`
- ✅ `SELECT u.*, p.* FROM users u JOIN profiles p ON u.id = p.user_id`
- ❌ `SELECT * FROM orders WHERE user_id = 1`

#### 2. 正则表达式规则 (REGEX)

基于 SQL 语句的正则表达式匹配。

```json
{
  "name": "report_query_cache",
  "description": "缓存报告查询",
  "type": "REGEX",
  "regex": ".*report.*|.*analytics.*",
  "ttl": "PT30M",
  "priority": 5,
  "enabled": true
}
```

**匹配示例**：
- ✅ `SELECT * FROM sales_report WHERE date = '2024-01-01'`
- ✅ `SELECT * FROM user_analytics WHERE period = 'daily'`
- ❌ `SELECT * FROM users WHERE status = 'active'`

#### 3. 查询类型规则 (QUERY_TYPE)

基于查询类型进行匹配。

```json
{
  "name": "select_only_cache",
  "description": "只缓存SELECT查询",
  "type": "QUERY_TYPE",
  "pattern": "SELECT",
  "ttl": "PT15M",
  "priority": 1,
  "enabled": true
}
```

**匹配示例**：
- ✅ `SELECT * FROM users`
- ✅ `SELECT COUNT(*) FROM orders`
- ❌ `INSERT INTO users (name) VALUES ('John')`
- ❌ `UPDATE users SET status = 'active'`

#### 4. 全局规则 (GLOBAL)

匹配所有查询的全局规则。

```json
{
  "name": "global_cache",
  "description": "全局缓存规则",
  "type": "GLOBAL",
  "ttl": "PT5M",
  "priority": 0,
  "enabled": true
}
```

**注意**：全局规则通常设置为最低优先级，作为兜底规则。

### 优先级规则

1. **数字越大优先级越高**
2. **相同优先级的规则按添加顺序执行**
3. **第一个匹配的规则生效，后续规则不再检查**
4. **事务中的查询自动跳过缓存，无需特殊规则配置**

## 🔧 API 使用示例

### 1. 创建表名规则

```bash
curl -X POST http://localhost:8010/api/cache/rules/table \
  -H "Content-Type: application/json" \
  -d '{
    "name": "products_cache",
    "description": "缓存产品表查询",
    "tables": ["products"],
    "ttl": "PT10M"
  }'
```

### 2. 创建正则表达式规则

```bash
curl -X POST http://localhost:8010/api/cache/rules/regex \
  -H "Content-Type: application/json" \
  -d '{
    "name": "dashboard_cache",
    "description": "缓存仪表板查询",
    "regex": ".*dashboard.*|.*summary.*",
    "ttl": "PT20M"
  }'
```

### 3. 获取所有规则

```bash
curl -X GET http://localhost:8010/api/cache/rules
```

### 4. 启用/禁用规则

```bash
curl -X PATCH http://localhost:8010/api/cache/rules/products_cache/toggle \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### 5. 删除规则

```bash
curl -X DELETE http://localhost:8010/api/cache/rules/products_cache
```

## 📊 监控和调试

### 1. 查看指标

访问 Prometheus 指标端点：

```bash
curl http://localhost:8010/metrics | grep ojp_cache
```

### 2. 关键指标说明

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `ojp_cache_hit_total` | Counter | 缓存命中次数 |
| `ojp_cache_miss_total` | Counter | 缓存未命中次数 |
| `ojp_cache_skip_total` | Counter | 缓存跳过次数 |
| `ojp_cache_processing_time_seconds` | Histogram | 缓存处理时间 |
| `ojp_transaction_start_total` | Counter | 事务开始次数 |
| `ojp_transaction_commit_total` | Counter | 事务提交次数 |
| `ojp_transaction_rollback_total` | Counter | 事务回滚次数 |

### 3. 日志调试

设置日志级别为 DEBUG 查看详细缓存决策：

```yaml
logging:
  level:
    org.openjdbcproxy.grpc.server.interceptor.impl.SmartCacheInterceptor: DEBUG
```

### 4. Grafana 仪表板

导入以下查询到 Grafana 创建监控仪表板：

```promql
# 缓存命中率
rate(ojp_cache_hit_total[5m]) / (rate(ojp_cache_hit_total[5m]) + rate(ojp_cache_miss_total[5m]))

# 缓存处理时间
histogram_quantile(0.95, rate(ojp_cache_processing_time_seconds_bucket[5m]))

# 事务统计
rate(ojp_transaction_start_total[5m])
```

## 🎯 最佳实践

### 1. 规则设计原则

- **精确匹配**：优先使用表名规则，避免过于宽泛的正则表达式
- **合理 TTL**：根据数据更新频率设置合适的缓存时间
- **优先级管理**：将特定规则设置为高优先级，通用规则设置为低优先级
- **事务安全**：系统自动检测事务状态，事务中的查询不会被缓存

### 2. 性能优化

- **规则数量**：避免创建过多规则，建议控制在 20 个以内
- **正则复杂度**：使用简单的正则表达式，避免复杂的回溯
- **缓存键设计**：合理设计缓存键，避免键冲突
- **监控告警**：设置缓存命中率告警，及时发现问题

### 3. 安全考虑

- **Redis 安全**：配置 Redis 认证和网络访问控制
- **规则验证**：定期检查规则配置，避免安全漏洞
- **数据敏感度**：避免缓存包含敏感信息的查询
- **访问控制**：限制规则管理 API 的访问权限

### 4. 故障排查

#### 常见问题

1. **规则不生效**
   - 检查规则是否启用
   - 验证规则优先级设置
   - 确认 SQL 语句匹配规则

2. **缓存命中率低**
   - 检查规则配置是否正确
   - 分析查询模式，优化规则
   - 确认 TTL 设置合理

3. **性能问题**
   - 检查 Redis 连接状态
   - 监控缓存处理时间
   - 优化规则匹配逻辑

#### 调试步骤

1. 启用 DEBUG 日志
2. 检查 Redis 连接
3. 验证规则配置
4. 分析指标数据
5. 查看缓存决策日志

## 📚 相关资源

- [设计文档](./SMART_CACHE_INTERCEPTOR_DESIGN.md)
- [API 文档](./README-REST-API.md)
- [配置示例](./cache-rules-example.json)
- [监控指标](../documents/telemetry/README.md)

## 🤝 支持

如果您在使用过程中遇到问题，请：

1. 查看日志文件获取详细错误信息
2. 检查配置是否正确
3. 参考故障排查指南
4. 提交 Issue 到项目仓库
