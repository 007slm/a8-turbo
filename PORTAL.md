# A8 Turbo 项目门户导航

## 核心服务入口

### 监控与可视化服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| Grafana | 3000 | [http://localhost:3000](http://localhost:3000) | 数据可视化平台，用于监控各项服务指标 |
| Prometheus | 9090 | [http://localhost:9090](http://localhost:9090) | 监控和告警工具，收集和存储时间序列数据 |
| NATS Dashboard | 8000 | [http://localhost:8000](http://localhost:8000) | NATS 消息系统的可视化监控面板 |
| Flink Dashboard | 8081 | [http://localhost:8081](http://localhost:8081) | Apache Flink 流处理框架的管理控制台 |

### 数据库与存储服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| MySQL | 3306 | localhost:3306 | 主数据库，存储业务数据 |
| Redis | 6379 | localhost:6379 | 缓存数据库，用于Redis Smart Cache |
| StarRocks | 9030/8030/8040/8080 | [http://localhost:9030](http://localhost:9030) 等 | OLAP数据库，用于数据分析 |

### 消息与流处理服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| NATS | 4222/8222 | localhost:4222, localhost:8222 | 消息队列服务，端口4222为客户端连接，8222为监控端点 |

### 大数据处理服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| StarRocks | 9030 | [http://localhost:9030](http://localhost:9030) | StarRocks MySQL服务器端口 |
| StarRocks | 8030 | [http://localhost:8030](http://localhost:8030) | StarRocks HTTP服务器端口 |
| StarRocks | 8040 | [http://localhost:8040](http://localhost:8040) | StarRocks Broker端口 |
| StarRocks | 8080 | [http://localhost:8080](http://localhost:8080) | StarRocks FE HTTP端口 |

### OJP服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| OJP Server | 1059 | [http://localhost:1059](http://localhost:1059) | OJP gRPC 服务端 |
| OJP Prometheus | 9026 | [http://localhost:9026](http://localhost:9026) | OJP服务监控端点 |
| OJP UI (开发) | 5173 | [http://localhost:5173](http://localhost:5173) | OJP前端开发服务器 |
| OJP UI (生产) | 50080 | [http://localhost:50080](http://localhost:50080) | OJP前端生产服务器 |

### Smart Cache演示服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| Smart Cache Demo | 8070 | [http://localhost:8070](http://localhost:8070) | Redis Smart Cache演示应用 |

## 项目结构说明

本项目包含多个 Docker Compose 配置文件：

1. [docker-compose.yml](file:///E:/a8-turbo/docker-compose.yml) - 主配置文件，包含基础监控服务
2. [docker-compose-cdc-sync.yml](file:///E:/a8-turbo/docker-compose-cdc-sync.yml) - CDC数据同步相关服务
3. [docker-compose-smart-cache.yml](file:///E:/a8-turbo/docker-compose-smart-cache.yml) - Redis Smart Cache演示环境
4. [docker-compose-ojp.yml](file:///E:/a8-turbo/docker-compose-ojp.yml) - OJP服务配置文件

## 常用操作命令

mklink /j e:\a8-turbo\ojp-server e:\ojp\ojp-server
mklink /j e:\a8-turbo\ojp-grpc-commons e:\ojp\ojp-grpc-commons
```

```
# 项目门户

## 访问地址

- 主门户: http://localhost:8000
- Grafana: http://localhost:8000/grafana
- Kong Admin API: http://localhost:8000/kong-admin
- Kong Manager: http://localhost:8002
- Prometheus: http://localhost:8000/prometheus
- NATS Dashboard: http://localhost:8000/nats-dashboard
- Redis Admin: http://localhost:8000/phpredisadmin

## 服务端口

- Kong Proxy: 8000
- Kong SSL Proxy: 8443
- Kong Admin API: 8001
- Kong SSL Admin API: 8444
- Kong Manager: 8002
- Kong Manager SSL: 8445
