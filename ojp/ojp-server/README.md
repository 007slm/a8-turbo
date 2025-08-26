# OJP Server - OpenJDBC Proxy Server

OJP Server 是一个高性能的数据库代理服务器，支持智能缓存、多协议访问和实时监控。

## 功能特性

- 🚀 **双协议支持**: 同时支持 gRPC 和 HTTP RESTful API
- 💾 **智能缓存**: 基于规则的智能缓存系统，支持表格、查询、正则表达式等多种缓存策略
- 📊 **实时监控**: 基于 Spring Boot Actuator 的完整系统监控
- 🖥️ **服务器管理**: 多服务器节点管理和控制
- 📈 **性能优化**: 查询性能分析、慢查询识别、缓存命中率统计
- 📝 **日志管理**: 完整的日志收集、查看和管理功能
- ⚙️ **系统设置**: 灵活的系统配置管理

## 技术栈

- **后端框架**: Spring Boot 3.x
- **协议支持**: gRPC + HTTP RESTful API
- **缓存引擎**: 自研智能缓存引擎
- **监控**: Spring Boot Actuator
- **构建工具**: Maven
- **Java版本**: Java 17+

## 快速开始

### 环境要求

- Java 17 或更高版本
- Maven 3.6+
- Redis (可选，用于分布式缓存)

### 安装和启动

#### 方式 1：使用启动脚本

**Windows:**
```batch
start-server.bat
```

**Linux/Mac:**
```bash
chmod +x start-server.sh
./start-server.sh
```

#### 方式 2：手动启动

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

#### 方式 3：使用 Maven

```bash
cd ojp/ojp-server
mvn spring-boot:run
```

### 端口配置

| 服务 | 配置项 | 默认端口 | 说明 |
|------|--------|----------|------|
| 统一服务端口 | `server.port` | 8010 | HTTP + gRPC 双协议端口 |
| Prometheus | `ojp.prometheus.port` | 9090 | 监控指标端口 |

**注意**: 现在使用单端口双协议模式，gRPC 和 HTTP 服务都在同一个端口上运行。

## API 文档

### RESTful API

OJP Server 提供完整的 RESTful API，与 Redis Smart Cache Web API 保持一致的接口设计。

#### 主要 API 类别

1. **系统状态相关接口** (`/api/actuator/*`)
   - 系统健康检查
   - 系统信息和指标
   - 环境配置信息

2. **服务器管理相关接口** (`/api/servers/*`)
   - 服务器列表管理
   - 服务器启动/停止/重启
   - 服务器状态监控

3. **缓存管理相关接口** (`/api/cache/*`)
   - 缓存概览统计
   - 缓存命中率统计
   - 查询性能统计
   - 热门表格统计
   - 慢查询统计

4. **规则管理相关接口** (`/api/rules/*`)
   - 缓存规则管理
   - 规则验证和提交
   - 规则状态管理

5. **日志管理相关接口** (`/api/logs/*`)
   - 应用日志查看
   - 访问日志查看
   - 错误日志查看
   - 日志文件下载

6. **系统设置相关接口** (`/api/settings/*`)
   - 系统配置管理
   - 缓存配置管理
   - 安全配置管理

#### 核心 API 端点

```bash
# 系统健康检查
GET /api/actuator/health

# 获取缓存统计
GET /api/cache/stats/overview

# 获取服务器列表
GET /api/servers

# 获取缓存规则
GET /api/rules

# 创建缓存规则
POST /api/rules
Content-Type: application/json

{
  "ttl": "30m",
  "ruleType": "TABLES",
  "tables": ["users", "orders"],
  "priority": 1
}
```

### Spring Boot Actuator 端点

OJP Server 集成了 Spring Boot Actuator，提供标准的监控端点：

```bash
# 系统健康状态
GET /actuator/health

# 系统信息
GET /actuator/info

# 系统指标
GET /actuator/metrics

# 特定指标
GET /actuator/metrics/{metricName}

# 环境信息
GET /actuator/env

# 配置属性
GET /actuator/configprops

# Bean 信息
GET /actuator/beans

# 线程转储
GET /actuator/threaddump
```

## 智能缓存系统

### 缓存规则类型

1. **表格规则 (TABLES)**: 基于表名的缓存策略
2. **查询规则 (QUERY)**: 基于特定查询的缓存策略
3. **正则规则 (REGEX)**: 基于正则表达式的缓存策略

### 缓存配置示例

```properties
# 缓存规则配置
cache.rules.enabled=true
cache.default.ttl=30m
cache.max.size=1000
cache.eviction.policy=LRU

# 表格缓存规则
cache.rules.tables.users.ttl=1h
cache.rules.tables.orders.ttl=30m

# 查询缓存规则
cache.rules.queries.select_users.ttl=15m
cache.rules.queries.select_orders.ttl=5m
```

## 项目结构

```
ojp-server/
├── src/main/java/org/openjdbcproxy/grpc/server/
│   ├── controller/          # REST API 控制器
│   │   ├── RestRuleController.java
│   │   ├── CacheStatsController.java
│   │   ├── ServerController.java
│   │   ├── LogController.java
│   │   └── SystemSettingsController.java
│   ├── smartcache/          # 智能缓存引擎
│   │   ├── api/
│   │   ├── engine/
│   │   └── service/
│   ├── config/              # 配置类
│   ├── model/               # 数据模型
│   └── GrpcServer.java      # 主启动类
├── src/main/resources/
│   ├── application.yml      # 应用配置
│   └── smart-cache-example.properties
├── README-REST-API.md       # 详细 API 文档
├── API_IMPLEMENTATION_STATUS.md  # API 实现状态
├── SMART_CACHE_README.md    # 智能缓存文档
├── REDIS_SETUP.md          # Redis 配置文档
└── start-server.sh         # 启动脚本
```

## 配置说明

### 核心配置

```yaml
server:
  port: 8010  # 统一服务端口

spring:
  application:
    name: ojp-server
  
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

ojp:
  cache:
    enabled: true
    default-ttl: 30m
    max-size: 1000
  redis:
    enabled: false
    host: localhost
    port: 6379
```

### 环境配置

可以创建环境特定的配置文件：
- `application-dev.yml` - 开发环境
- `application-prod.yml` - 生产环境
- `application-test.yml` - 测试环境

## 部署说明

### Docker 部署

```dockerfile
FROM openjdk:17-jre-slim
COPY ojp-server.jar /app/
WORKDIR /app
EXPOSE 8010
CMD ["java", "-jar", "ojp-server.jar"]
```

### 生产环境建议

1. **JVM 调优**:
   ```bash
   java -Xms2g -Xmx4g -XX:+UseG1GC -jar ojp-server.jar
   ```

2. **监控配置**:
   - 集成 Prometheus + Grafana
   - 配置日志收集 (ELK Stack)
   - 设置告警规则

3. **安全配置**:
   - 配置 CORS 策略
   - 启用 SSL/TLS
   - 设置访问控制

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

## 测试

### API 测试

使用提供的测试脚本：

```bash
# Windows
test-api.bat

# Linux/Mac
./test-api.sh
```

### 手动测试

```bash
# 系统健康检查
curl http://localhost:8010/api/actuator/health

# 获取缓存统计
curl http://localhost:8010/api/cache/stats/overview

# 获取服务器列表
curl http://localhost:8010/api/servers
```



