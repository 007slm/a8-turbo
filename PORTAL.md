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

## 项目结构说明

本项目包含多个 Docker Compose 配置文件：

1. [docker-compose.yml](file:///E:/a8-turbo/docker-compose.yml) - 主配置文件，包含基础监控服务
2. [docker-compose-cdc-sync.yml](file:///E:/a8-turbo/docker-compose-cdc-sync.yml) - CDC数据同步相关服务
3. [docker-compose-smart-cache.yml](file:///E:/a8-turbo/docker-compose-smart-cache.yml) - Redis Smart Cache演示环境

## 常用操作命令

### 启动服务

```bash
# 启动所有服务
docker-compose up

# 后台启动所有服务
docker-compose up -d

# 启动特定服务
docker-compose up grafana prometheus
```

### 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除所有相关卷
docker-compose down -v
```

### 查看服务状态

```bash
# 查看运行中的服务
docker-compose ps

# 查看服务日志
docker-compose logs <service_name>
```

## 注意事项

1. 首次启动可能需要较长时间下载镜像
2. 确保端口未被其他应用占用
3. 如遇到网络问题，可尝试创建Docker网络：
   ```bash
   docker network create a8
   ```
4. Grafana仪表板可能需要几分钟时间才能显示数据
5. 如果Grafana仪表板无数据，请确认使用的是9.5.2及以上版本

## 技术栈

- **后端**: Java 17
- **数据库**: MySQL, Redis
- **缓存技术**: Redis Smart Cache (自定义JDBC驱动)
- **消息队列**: NATS
- **监控**: Prometheus, Grafana, JMX, Redis TimeSeries
- **构建工具**: Gradle 8.x
- **流处理**: Apache Flink
- **OLAP数据库**: StarRocks