# OJP Server RESTful API

OJP Server 现在支持 RESTful API，与 Redis Smart Cache Web API 保持一致的接口设计。

## 功能特性

- **双协议支持**：同时支持 gRPC 和 HTTP RESTful API
- **接口兼容**：与 Redis Smart Cache Web API 保持一致的接口
- **RESTful 规范**：遵循标准 RESTful API 设计最佳实践
- **无需额外端口**：HTTP 服务器作为 gRPC 服务器的补充服务
- **前端兼容**：前端代码可以无缝迁移，无需修改

## 启动方式

### 方式 1：使用启动脚本

#### Windows
```batch
start-server.bat
```

#### Linux/Mac
```bash
chmod +x start-server.sh
./start-server.sh
```

### 方式 2：手动启动

```bash
# 单端口双协议模式
java -Dserver.port=8010 \
     -Dojp.prometheus.port=9090 \
     -jar ojp-server-0.0.8-alpha.jar

# 或者使用环境变量
export OJP_UNIFIED_PORT=8010
export OJP_PROMETHEUS_PORT=9090
java -jar ojp-server-0.0.8-alpha.jar
```

## 端口配置

| 服务 | 配置项 | 默认端口 | 说明 |
|------|--------|----------|------|
| 统一服务端口 | `server.port` | 8010 | HTTP + gRPC 双协议端口 |
| Prometheus | `ojp.prometheus.port` | 9090 | 监控指标端口 |

**注意**：现在使用单端口双协议模式，gRPC 和 HTTP 服务都在同一个端口上运行。

## API 接口

### 规则管理 API

#### 获取所有规则
```
GET /api/rules
```

#### 创建规则
```
POST /api/rules
Content-Type: application/json

{
  "ttl": "30m",
  "ruleType": "TABLES",
  "tables": ["users", "orders"],
  "priority": 1
}
```

#### 更新规则
```
PUT /api/rules/{ruleId}
Content-Type: application/json

{
  "ttl": "1h",
  "ruleType": "TABLES",
  "tables": ["users", "orders", "products"],
  "priority": 2
}
```

#### 删除规则
```
DELETE /api/rules/{ruleId}
```

#### 获取规则状态
```
GET /api/rules/{ruleId}/status
```

#### 更新规则状态
```
PUT /api/rules/{ruleId}/status
Content-Type: application/json

{
  "enabled": true
}
```

#### 批量提交规则
```
POST /api/rules/commit
Content-Type: application/json

{
  "rules": [
    {
      "ttl": "30m",
      "ruleType": "TABLES",
      "tables": ["users"]
    },
    {
      "ttl": "1h",
      "ruleType": "REGEX",
      "regex": "SELECT.*FROM orders"
    }
  ]
}
```

#### 验证规则
```
POST /api/rules/validate
Content-Type: application/json

{
  "ttl": "30m",
  "ruleType": "TABLES",
  "tables": ["users"]
}
```

#### 规则健康检查
```
GET /api/rules/health
```

### 缓存统计 API

#### 缓存概览统计
```
GET /api/cache/stats/overview
```

#### 缓存命中率统计
```
GET /api/cache/stats/hit-rate
```

#### 查询性能统计
```
GET /api/cache/stats/query-performance
```

#### 热门表格统计
```
GET /api/cache/stats/top-tables
```

#### 慢查询统计
```
GET /api/cache/stats/slow-queries
```

### 查询管理 API

#### 获取查询列表
```
GET /api/cache/queries
```

#### 获取特定查询的缓存规则
```
GET /api/cache/queries/{queryId}/rules
```

#### 为特定查询创建缓存规则
```
POST /api/cache/queries/{queryId}/rules
Content-Type: application/json

{
  "ttl": "30m",
  "ruleType": "QUERY",
  "priority": 1
}
```

### 表格管理 API

#### 获取表格列表
```
GET /api/cache/tables
```

#### 获取特定表格的缓存规则
```
GET /api/cache/tables/{tableName}/rules
```

#### 为特定表格创建缓存规则
```
POST /api/cache/tables/{tableName}/rules
Content-Type: application/json

{
  "ttl": "1h",
  "ruleType": "TABLE",
  "priority": 1
}
```

#### 获取表格统计
```
GET /api/cache/tables/{tableName}/stats
```

### 服务器管理 API

#### 获取服务器列表
```
GET /api/servers
```

#### 创建服务器
```
POST /api/servers
Content-Type: application/json

{
  "name": "MySQL Server",
  "host": "localhost",
  "port": 3306,
  "databaseType": "mysql",
  "username": "root"
}
```

#### 获取服务器详情
```
GET /api/servers/{serverId}
```

#### 更新服务器
```
PUT /api/servers/{serverId}
Content-Type: application/json

{
  "name": "Updated MySQL Server",
  "host": "localhost",
  "port": 3306
}
```

#### 删除服务器
```
DELETE /api/servers/{serverId}
```

#### 启动服务器
```
POST /api/servers/{serverId}/start
```

#### 停止服务器
```
POST /api/servers/{serverId}/stop
```

#### 重启服务器
```
POST /api/servers/{serverId}/restart
```

#### 获取服务器状态
```
GET /api/servers/{serverId}/status
```

#### 获取服务器日志
```
GET /api/servers/{serverId}/logs?level=INFO&limit=100
```



### 系统监控

OJP Server 集成了 Spring Boot Actuator，提供完整的系统监控功能。前端可以直接调用 Actuator 的端点：

#### 系统健康检查
```
GET /actuator/health
```

#### 系统信息
```
GET /actuator/info
```

#### 系统指标
```
GET /actuator/metrics
```

#### 特定指标
```
GET /actuator/metrics/{metricName}
```

#### 环境信息
```
GET /actuator/env
```

#### 配置属性
```
GET /actuator/configprops
```

#### Bean 信息
```
GET /actuator/beans
```

#### 线程转储
```
GET /actuator/threaddump
```

**注意**：这些端点由 Spring Boot Actuator 提供，无需额外的控制器实现。前端可以直接调用这些标准的 Actuator 端点。

### 日志管理 API

#### 获取应用日志
```
GET /api/logs/application?level=INFO&limit=100&startTime=2024-01-01T00:00:00Z
```

#### 获取访问日志
```
GET /api/logs/access?limit=100&startTime=2024-01-01T00:00:00Z
```

#### 获取错误日志
```
GET /api/logs/error?limit=100&startTime=2024-01-01T00:00:00Z
```

#### 下载日志文件
```
GET /api/logs/{logType}/download?date=2024-01-01
```

#### 清理日志
```
POST /api/logs/{logType}/clear
Content-Type: application/json

{
  "beforeDate": "2024-01-01T00:00:00Z"
}
```

### 系统设置 API

#### 获取系统配置
```
GET /api/settings/system
```

#### 更新系统配置
```
PUT /api/settings/system
Content-Type: application/json

{
  "maxConnections": 100,
  "timeout": 30000,
  "enableCache": true
}
```

#### 获取缓存配置
```
GET /api/settings/cache
```

#### 更新缓存配置
```
PUT /api/settings/cache
Content-Type: application/json

{
  "defaultTtl": "1h",
  "maxSize": 1000,
  "evictionPolicy": "LRU"
}
```

#### 获取安全配置
```
GET /api/settings/security
```

#### 更新安全配置
```
PUT /api/settings/security
Content-Type: application/json

{
  "enableSsl": true,
  "allowedOrigins": ["http://localhost:3000"],
  "sessionTimeout": 3600
}
```

## RESTful API 设计原则

### 1. 资源导向设计
- 使用名词而非动词：`/api/rules` 而不是 `/api/create-rule`
- 资源层次结构清晰：`/api/cache/tables/{tableName}/rules`
- 使用复数形式表示资源集合：`/api/rules`、`/api/servers`

### 2. HTTP 方法语义
- `GET`：获取资源
- `POST`：创建资源
- `PUT`：更新资源（完整更新）
- `PATCH`：部分更新资源
- `DELETE`：删除资源

### 3. 状态码使用
- `200 OK`：请求成功
- `201 Created`：资源创建成功
- `204 No Content`：请求成功但无返回内容
- `400 Bad Request`：请求参数错误
- `401 Unauthorized`：未授权
- `403 Forbidden`：禁止访问
- `404 Not Found`：资源不存在
- `500 Internal Server Error`：服务器内部错误

### 4. 响应格式统一
```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": 1640995200000
}
```

### 5. 错误处理
```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "错误描述",
  "timestamp": 1640995200000
}
```

## 与 Redis Smart Cache Web API 的区别

### 保持一致的特性
- API 路径和 HTTP 方法
- 请求/响应数据结构
- 错误处理机制
- 验证逻辑

### 技术差异
- **存储后端**：使用 OJP 的内存存储，而非 Redis
- **规则引擎**：使用 OJP 的 CacheRuleEngine
- **服务层**：基于 SmartCacheRuleService 实现

### 改进的 RESTful 设计
- **资源路径优化**：`/api/rules` 替代 `/api/cache/rules`
- **嵌套资源**：`/api/cache/queries/{queryId}/rules` 替代 `/api/cache/queries/create-rule`
- **状态管理**：`/api/rules/{id}/status` 替代 `/api/cache/rules/{enabled}`
- **统一命名**：使用复数形式表示资源集合

## 前端迁移指南

### 1. 更新 API 基础 URL
```javascript
// 原来
const API_BASE = 'http://localhost:8080/api';

// 现在（如果端口不同）
const API_BASE = 'http://localhost:8010/api'; // 或者你的 OJP 服务器端口
```

### 2. 更新 API 调用路径
```javascript
// 原来的路径
const rules = await fetch('/api/cache/rules').then(r => r.json());

// 新的路径
const rules = await fetch('/api/rules').then(r => r.json());

// 原来的路径
const result = await fetch('/api/cache/queries/create-rule', {
  method: 'POST',
  body: JSON.stringify(ruleData)
}).then(r => r.json());

// 新的路径
const result = await fetch(`/api/cache/queries/${queryId}/rules`, {
  method: 'POST',
  body: JSON.stringify(ruleData)
}).then(r => r.json());
```

### 3. 处理响应格式
```javascript
// 响应格式保持一致
if (response.success) {
  // 处理成功响应
  const data = response.data;
} else {
  // 处理错误
  console.error(response.message);
}
```

## 监控和日志

### 访问日志
HTTP 服务器的访问日志会记录在标准输出中，可以通过以下方式查看：
```bash
java -Dojp.http.port=8010 -jar ojp-server.jar | grep "HTTP"
```

### 健康检查
```
GET /api/actuator/health
```

## 故障排除

### 常见问题

1. **端口冲突**
   - 确保 8010 端口未被占用
   - 使用 `-Dserver.port=8010` 指定其他端口

2. **启动失败**
   - 检查 Java 版本（需要 Java 17+）
   - 确保所有依赖都已正确安装

3. **API 无响应**
   - 检查 HTTP 服务器是否正常启动
   - 查看启动日志中的端口信息

### 调试模式
```bash
java -Dserver.port=8010 \
     -Dlogging.level.org.openjdbcproxy=DEBUG \
     -jar ojp-server.jar
```

## 性能考虑

- HTTP 服务器运行在独立的线程中，不影响 gRPC 性能
- 使用内存存储，响应速度快
- 支持 CORS，便于前端开发

## 安全建议

- 在生产环境中配置适当的 CORS 策略
- 考虑添加认证和授权机制
- 限制允许的 IP 地址范围

## 项目配置说明

### 配置文件整理

项目已整理为单一配置文件 `application.yml`，包含以下配置：

#### 核心配置
- **统一服务端口**: 端口 8010，同时支持 HTTP REST API 和 gRPC
- **Redis配置**: 连接池和超时设置

#### API配置
- **基础路径**: `/api`
- **缓存路径**: `/api/cache`
- **监控端点**: `/api/actuator`

#### 安全配置
- **CORS**: 支持跨域请求
- **IP限制**: 可配置允许的IP地址
- **CSRF**: gRPC服务禁用CSRF保护

### 启动类

项目现在只有一个主启动类：`GrpcServer.java`

- 使用 `@SpringBootApplication` 注解
- 自动配置 HTTP 和 gRPC 服务
- 支持优雅关闭和健康检查
- 集成监控和日志功能

### 启动方式

```bash
# 使用 Maven
mvn spring-boot:run

# 使用 Java
java -jar ojp-server.jar

# 指定统一端口（HTTP + gRPC）
java -Dserver.port=8010 -jar ojp-server.jar

# 使用启动脚本
./start-server.sh  # Linux/Mac
start-server.bat   # Windows
```

### 配置优先级

1. 命令行参数 (`-D` 参数)
2. 环境变量
3. `application.yml` 配置文件
4. Spring Boot 默认值

### 环境配置

可以创建环境特定的配置文件：
- `application-dev.yml` - 开发环境
- `application-prod.yml` - 生产环境
- `application-test.yml` - 测试环境

## 单端口双协议配置

### 技术原理

Spring gRPC 的 `spring-grpc-server-web-spring-boot-starter` 支持在同一个端口上同时处理 HTTP 和 gRPC 请求：

- **HTTP 请求**: 通过标准的 Servlet 容器处理
- **gRPC 请求**: 通过 HTTP/2 协议处理，自动路由到对应的 gRPC 服务
- **协议识别**: 自动识别请求类型，无需额外配置

### 配置要点

1. **移除独立 gRPC 端口**：
   ```yaml
   grpc:
     server:
       # port: 1059  # 注释掉，使用 HTTP 服务器端口
   ```

2. **统一端口配置**：
   ```yaml
   server:
     port: 8010  # HTTP + gRPC 统一端口
   ```

3. **启动参数简化**：
   ```bash
   # 原来需要两个端口
   java -Dojp.server.port=1059 -Dojp.http.port=8010 -jar ojp-server.jar
   
   # 现在只需要一个端口
   java -Dserver.port=8010 -jar ojp-server.jar
   ```

### 优势

- **简化部署**: 只需要管理一个端口
- **减少资源**: 避免端口冲突和资源浪费
- **统一管理**: 监控、日志、安全配置统一
- **兼容性好**: 完全兼容现有的 HTTP 和 gRPC 客户端

## 依赖问题解决

### Lettuce Redis 客户端版本问题

**问题描述**：
```
[ERROR] io.lettuce:lettuce-core:jar:6.3.1 was not found in https://repo.maven.apache.org/maven2
```

**原因分析**：
- Lettuce 6.3.1 版本在 Maven 中央仓库中不存在
- 可能是版本号错误或该版本已被移除

**解决方案**：
- 将版本降级到稳定版本：`6.1.8.RELEASE`
- 修复了 `RedisConnectionManager.java` 中的 SSL 配置问题

**修复后的配置**：
```xml
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
    <version>6.1.8.RELEASE</version>
</dependency>
```

**代码修复**：
```java
// 修复前（错误）
if (redisConfig.isSsl()) {
    uriBuilder.withSsl();  // 缺少参数
}

// 修复后（正确）
if (redisConfig.isSsl()) {
    uriBuilder.withSsl(true);  // 添加 boolean 参数
}
```

**验证方法**：
```bash
# 跳过测试，只编译主代码
mvn clean compile -DskipTests

# 完整构建（包含测试）
mvn clean package
```

**注意事项**：
- 测试代码存在一些兼容性问题，建议先跳过测试进行主代码编译
- 主代码编译成功后，可以逐步修复测试代码
- 如果遇到其他依赖问题，可以尝试使用 `-U` 参数强制更新依赖
