# A8 Turbo 项目门户导航

## 核心服务入口

### 监控与可视化服务

| 服务名称 | 端口 | 访问地址 | 描述 |
|---------|------|---------|------|
| Grafana | 3000 | [http://localhost:3000](http://localhost:3000) | 数据可视化平台，用于监控各项服务指标 |
| Prometheus | 9090 | [http://localhost:9090](http://localhost:9090) | 监控和告警工具，收集和存储时间序列数据 |
| NATS Dashboard | 8000 | [http://localhost:8000](http://localhost:8000) | NATS 消息系统的可视化监控面板 |
| SeaTunnel Zeta Master | 8080 | [http://localhost:8080](http://localhost:8080) | SeaTunnel Zeta 集群管理 REST 控制台（Swagger: `/swagger-ui/index.html`） |

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
| SeaTunnel Zeta Master (REST) | 8080 | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | SeaTunnel Zeta 集群管理 REST 接口，可查询/管理作业 |
| SeaTunnel Zeta Master (RPC) | 5801 | `seatunnel-master:5801` | SeaTunnel Zeta 集群 RPC 端口，供 worker 与 submitter 通信 |

SeaTunnel 作业现在通过 OJP 系统动态创建和管理，不再使用静态配置文件。


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

### ShopService 演示接口

| 功能 | 入口 | 描述 |
|------|------|------|
| 管理后台入口 | [http://localhost:5173/#/shopservice/users](http://localhost:5173/#/shopservice/users) | React 门户中的 ShopService 管理界面 |
| Chinook SQL 实验台 | [http://localhost:5173/#/shopservice/chinook](http://localhost:5173/#/shopservice/chinook) | 预置多组复杂查询模板，点击按钮即可运行并校验 OJP |
| REST: GET /shop/chinook/tables | `/shop/chinook/tables` | 通过 Kong 代理访问表结构（默认代理前缀 `/shop`） |
| REST: GET /shop/chinook/sample-queries | `/shop/chinook/sample-queries` | 查询预置示例 SQL |
| REST: POST /shop/chinook/query | `/shop/chinook/query` | 执行 SELECT/WITH 查询，返回行数据与列元信息 |

## 项目结构说明

本项目包含多个 Docker Compose 配置文件：

1. [docker-compose.yml](file:///E:/a8-turbo/docker-compose.yml) - 主配置文件，包含基础监控服务
2. [docker-compose-smart-cache.yml](file:///E:/a8-turbo/docker-compose-smart-cache.yml) - Redis Smart Cache演示环境
3. [docker-compose-ojp.yml](file:///E:/a8-turbo/docker-compose-ojp.yml) - OJP服务配置文件
5. [docker-compose-cdc-sync-zeta.yml](file:///E:/a8-turbo/docker-compose-cdc-sync-zeta.yml) - SeaTunnel Zeta 集群（master/worker/submitter）与 CDC 作业编排

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
- Redis Admin: http://localhost:8000/phpredmin
- SeaTunnel Zeta Master: http://localhost:8080/swagger-ui/index.html

## 服务端口

- Kong Proxy: 8000
- Kong SSL Proxy: 8443
- Kong Admin API: 8001
- Kong SSL Admin API: 8444
- Kong Manager: 8002
- Kong Manager SSL: 8445
