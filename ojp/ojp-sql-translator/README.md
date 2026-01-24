# OJP Server

OJP Server 是 Open JDBC Proxy 的核心服务模块，基于 Spring Boot 提供高性能的 gRPC 和 HTTP 服务。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 文档](#api-文档)
- [监控和管理](#监控和管理)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- **双协议支持**: 同时支持 gRPC 和 HTTP 协议
- **智能路由**: 基于表同步状态的查询路由决策
- **连接池管理**: HikariCP 连接池，支持多种数据库
- **事务管理**: 分布式事务支持
- **会话管理**: UUID 基础的客户端会话跟踪
- **性能监控**: 内置指标收集和健康检查
- **慢查询隔离**: 自动识别并隔离慢查询到专用连接池

### 技术特性

- **Spring Boot 3.3.3**: 现代化 Spring 生态
- **gRPC**: 高性能 RPC 通信
- **JDBC 4.3**: 完整 JDBC 规范支持
- **连接池**: HikariCP 高性能连接池
- **监控**: Spring Boot Actuator
- **配置**: 外部化配置支持

## 🏗️ 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                     OJP Server                           │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │  gRPC Service│ Cache Engine │  Monitor/Actuator  │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
│                                                         │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │ Connection   │ Session      │ Transaction         │  │
│  │ Pool Manager │ Manager      │ Manager             │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼────────┐┌─▼──────────┐┌─▼──────────────┐
│   JDBC Client  ││  MySQL      ││  StarRocks    │
│   (ojp-driver) ││  (Source)   ││  (Cache)      │
└────────────────┘└─────────────┘└────────────────┘
```

### 核心服务类

- **GrpcServer**: 主启动类
- **StatementServiceImpl**: gRPC 服务实现
- **ConnectionManager**: 数据库连接管理
- **SessionManager**: 客户端会话管理
- **SlowQuerySegregationManager**: 慢查询隔离
- **CircuitBreaker**: 故障转移保护

## 🚀 快速开始

### 环境要求

- **Java**: 11+
- **Maven**: 3.8+
- **Redis**: 6.0+
- **数据库**: MySQL/StarRocks

### 配置数据库

在 `application.yml` 中配置数据源：

```yaml
spring:
  datasource:
    mysql:
      url: jdbc:mysql://localhost:3306/mydb
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
    starrocks:
      url: jdbc:mysql://localhost:9030/mydb
      username: root
      password: ""
      driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
```

### 构建和运行

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/ojp-server-1.0.jar
```

服务将在端口 8010 启动，支持：
- HTTP API: http://localhost:8010/api/*
- gRPC 服务: localhost:8010
- 健康检查: http://localhost:8010/actuator/health

## ⚙️ 配置说明

### 核心配置

```yaml
ojp:
  server:
    port: 8010
    grpc:
      enabled: true
    http:
      enabled: true

  database:
    mysql:
      pool:
        maximum-pool-size: 20
        minimum-idle: 5
    starrocks:
      pool:
        maximum-pool-size: 10
        minimum-idle: 2

  cache:
    decision:
      enabled: true
    slow-query:
      threshold: 1000  # 毫秒

  session:
    timeout: 3600000  # 1小时
```

### 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `OJP_SERVER_PORT` | 服务端口 | `8010` |
| `OJP_REDIS_HOST` | Redis 主机 | `localhost` |
| `OJP_REDIS_PORT` | Redis 端口 | `6379` |
| `SPRING_PROFILES_ACTIVE` | Spring 环境 | `default` |

## 📚 API 文档

### HTTP API

- **健康检查**: `GET /actuator/health`
- **指标监控**: `GET /actuator/metrics`
- **查询统计**: `GET /api/cache/queries/list`
- **表状态**: `GET /api/cache/tables/status`

### gRPC 服务

主要服务：
- `StatementService`: SQL 语句执行
- `ConnectionService`: 数据库连接管理
- `TransactionService`: 事务管理

详细 API 文档参考 `docs/RESTful API设计文档.md` 和 `docs/Service层接口设计文档.md`。

## 📊 监控和管理

### Spring Boot Actuator

启用端点：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 关键指标

- **连接池状态**: HikariCP 连接池指标
- **查询性能**: 执行时间分布
- **缓存命中率**: 路由决策统计
- **会话管理**: 活跃会话数量

### 监控端点

- **Prometheus**: `/actuator/prometheus`
- **健康状态**: `/actuator/health`
- **应用信息**: `/actuator/info`

## 🔧 故障排查

### 常见问题

#### 1. 服务启动失败

**可能原因**:
- 端口被占用
- 数据库连接失败
- Redis 连接异常

**解决方法**:
```bash
# 检查端口占用
netstat -tlnp | grep 8010

# 检查数据库连接
telnet localhost 3306

# 查看启动日志
tail -f logs/ojp-server.log
```

#### 2. 查询路由异常

**可能原因**:
- 表同步状态不准确
- 缓存决策服务异常

**解决方法**:
- 检查 CDC 监控状态
- 验证 Redis 中表状态数据
- 重启缓存决策服务

#### 3. 性能问题

**可能原因**:
- 连接池配置不当
- 慢查询过多
- 内存不足

**解决方法**:
- 调整连接池参数
- 启用慢查询隔离
- 增加 JVM 内存配置

## 🤝 开发指南

### 项目结构

```
ojp-server/
├── src/main/java/org/openjdbcproxy/
│   ├── grpc/server/          # gRPC 服务实现
│   ├── server/               # Spring Boot 配置
│   ├── cache/                # 缓存决策集成
│   └── common/               # 通用工具类
├── src/main/resources/
│   ├── application.yml       # 主配置文件
│   └── logback-spring.xml    # 日志配置
└── docs/                     # 详细文档
```

### 扩展开发

1. **添加新的数据库支持**: 实现 `DatabaseDialect` 接口
2. **自定义路由策略**: 扩展 `CacheDecisionService`
3. **增加监控指标**: 使用 Micrometer 注册自定义指标

## 📄 许可证

本项目采用 MIT 许可证。</content>
<parameter name="filePath">E:\a8-turbo\ojp\ojp-server\README.md