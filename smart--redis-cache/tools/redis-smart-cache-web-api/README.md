# Redis Smart Cache Web API

Redis Smart Cache Web API 是一个基于 Spring Boot 3 的 RESTful API 服务，为 Redis Smart Cache 提供 HTTP 接口，配合前端 UI 实现缓存规则的可视化管理。

## 🚀 功能特性

### 核心功能
- **查询管理**: 查看数据库查询信息，支持排序、分页、搜索
- **表格管理**: 查看数据表统计信息，创建表级缓存规则
- **规则管理**: 完整的 CRUD 操作，支持多种规则类型
- **统计监控**: 提供丰富的统计数据和监控指标
- **配置管理**: 动态配置管理和连接测试

### 技术特性
- ✅ 基于 Spring Boot 3.2.0
- ✅ Java 17 支持
- ✅ OpenAPI 3.0 文档 (Swagger UI)
- ✅ CORS 跨域支持
- ✅ 全局异常处理
- ✅ 多环境配置
- ✅ 健康检查和监控

## 📋 系统要求

- **Java**: 17+
- **Maven**: 3.6+
- **Redis**: 6.0+ (支持 Redis Stack)
- **Redis Smart Cache**: 0.3.3+

## 🛠️ 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd redis-smart-cache-web-api
```

### 2. 配置环境变量 (可选)

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your_password
export SMARTCACHE_APP_NAME=smartcache
```

### 3. 编译项目

```bash
mvn clean compile
```

### 4. 运行应用

```bash
mvn spring-boot:run
```

或者编译为 JAR 文件后运行：

```bash
mvn clean package
java -jar target/redis-smart-cache-web-api.jar
```

### 5. 访问应用

- **API 服务**: http://localhost:8080
- **API 文档**: http://localhost:8080/swagger-ui.html  
- **健康检查**: http://localhost:8080/actuator/health

## 🔧 配置说明

### Redis 连接配置

在 `application.yml` 中配置 Redis 连接信息：

```yaml
smartcache:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: ""
    username: default
    ssl: false
    timeout: 10000
  application:
    name: smartcache
```

### 环境变量配置

支持以下环境变量：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `REDIS_HOST` | localhost | Redis 主机地址 |
| `REDIS_PORT` | 6379 | Redis 端口 |
| `REDIS_DATABASE` | 0 | Redis 数据库编号 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `REDIS_USERNAME` | default | Redis 用户名 |
| `REDIS_SSL` | false | 是否启用 SSL |
| `SMARTCACHE_APP_NAME` | smartcache | Smart Cache 应用名称 |

## 📖 API 文档

### 接口分组

1. **Redis 连接管理** (`/api/redis/**`)
   - 连接测试、状态查询、健康检查

2. **查询管理** (`/api/queries/**`)
   - 查询列表、详情、搜索、规则创建

3. **表格管理** (`/api/tables/**`)
   - 表格列表、统计、规则创建

4. **规则管理** (`/api/rules/**`)
   - 规则 CRUD、批量操作、验证

5. **统计监控** (`/api/stats/**`)
   - 总体统计、性能指标、趋势数据

6. **配置管理** (`/api/config/**`)
   - 配置查询、更新、重置

### 主要接口示例

#### 获取查询列表
```bash
GET /api/queries?sortBy=queryTime&sortDirection=DESC&limit=10
```

#### 创建缓存规则
```bash
POST /api/rules
Content-Type: application/json

{
  "ttl": "30m",
  "ruleType": "TABLES_ANY",
  "tablesAny": ["users", "orders"]
}
```

#### 获取统计信息
```bash
GET /api/stats/overview
```

## 🏗️ 项目结构

```
src/main/java/com/redis/smartcache/webapi/
├── config/                 # 配置类
│   ├── SmartCacheRedisConfig.java    # Redis 配置
│   ├── WebConfig.java               # Web 配置 (CORS)
│   ├── OpenApiConfig.java          # API 文档配置
│   └── GlobalExceptionHandler.java # 全局异常处理
├── controller/             # 控制器层
│   ├── RedisConnectionController.java
│   ├── QueryController.java
│   ├── TableController.java
│   ├── RuleController.java
│   ├── StatsController.java
│   └── ConfigController.java
├── model/                  # 数据模型
│   ├── QueryInfo.java
│   ├── TableInfo.java
│   ├── RuleInfo.java
│   ├── StatsModels.java
│   └── ApiModels.java
├── service/                # 服务层
│   ├── RedisSmartCacheService.java
│   └── impl/
│       └── RedisSmartCacheServiceImpl.java
└── RedisSmartCacheWebApiApplication.java  # 启动类
```

## 🔄 与前端 UI 集成

此 API 服务专门设计用于配合 Redis Smart Cache UI 项目：

1. **前端项目路径**: `tools/redis-smart-cache-ui-bytecode`
2. **API 基础路径**: `http://localhost:8080/api`
3. **CORS 配置**: 已配置允许前端跨域访问

### 启动完整系统

1. 启动 Redis 和 Smart Cache 服务
2. 启动 Web API 服务 (端口 8080)
3. 启动前端 UI 服务 (通常端口 3000)

## 🧪 开发和测试

### 开发模式

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 运行测试

```bash
mvn test
```

### 构建生产版本

```bash
mvn clean package -Pprod
```

## 📊 监控和运维

### Actuator 端点

- `/actuator/health` - 健康检查
- `/actuator/info` - 应用信息  
- `/actuator/metrics` - 应用指标
- `/actuator/prometheus` - Prometheus 指标

### 日志配置

日志文件位置: `logs/redis-smart-cache-web-api.log`

可通过配置文件调整日志级别：

```yaml
logging:
  level:
    com.redis.smartcache: DEBUG
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目使用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持

如有问题或建议，请：

1. 查看 [API 文档](http://localhost:8080/swagger-ui.html)
2. 提交 [Issue](https://github.com/redis-field-engineering/redis-smart-cache/issues)
3. 联系 Redis Smart Cache 团队