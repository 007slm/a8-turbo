# OJP CDC Monitor Module

SeaTunnel Event Handler SPI 插件，用于 `ojp-cache` 实时 CDC 状态监控。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [事件监听](#事件监听)
- [配置说明](#配置说明)
- [部署指南](#部署指南)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- **实时 CDC 状态监控**: 监听 SeaTunnel 引擎事件，实时推送 CDC 状态
- **智能缓存决策**: 将 CDC 状态推送到 Redis，供 `ojp-cache` 做缓存决策
- **自动上下文获取**: 首次接收事件时，通过 REST API 获取 Job 详情并缓存
- **心跳检测**: 定期更新心跳，确保 CDC 任务正常运行

### 支持的事件

| 事件 | 触发时机 | 行为 |
|:---|:---|:---|
| `LIFECYCLE_READER_CLOSE` | Snapshot 完成 | 标记 `phase=INCREMENTAL`，发送 `SNAPSHOT_DONE` |
| `CHECKPOINT_COMPLETED` | Checkpoint 完成 | 更新心跳，发送 `CHECKPOINT_OK` |

## 🏗️ 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                  SeaTunnel Engine                        │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │  Snapshot    │  Incremental │   Checkpoint        │  │
│  │   Phase      │    Phase     │     Manager         │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │ Event
┌────────────────────┴────────────────────────────────────┐
│              ojp-cdc-monitor (SPI Plugin)                │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │ Event Handler│ Job Context  │  Redis Publisher    │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                      Redis                               │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │  CDC Status  │  Job Context │   Heartbeat Data    │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   ojp-cache                              │
│              (读取 CDC 状态做缓存决策)                    │
└─────────────────────────────────────────────────────────┘
```

### 数据流向

1. SeaTunnel 引擎触发事件（Snapshot 完成、Checkpoint 完成）
2. ojp-cdc-monitor 接收事件
3. 首次接收时，通过 REST API 获取 Job 详情
4. 将 CDC 状态推送到 Redis
5. ojp-cache 从 Redis 读取 CDC 状态，做缓存决策

## 🚀 快速开始

### 环境要求

- **Java**: 11+
- **SeaTunnel**: 2.3.x+
- **Redis**: 6.0+

### 构建

```bash
cd ojp-cdc-monitor
mvn clean package
```

### 配置环境变量

```bash
export OJP_REDIS_HOST=redis
export OJP_REDIS_PORT=6379
export OJP_REDIS_AUTH=
export OJP_SEATUNNEL_API_URL=http://seatunnel-master:8080
```

## 📡 事件监听

### LIFECYCLE_READER_CLOSE

**触发时机**: Snapshot 阶段完成

**行为**:
- 标记 CDC 阶段为 `INCREMENTAL`
- 发送 `SNAPSHOT_DONE` 事件到 Redis
- 更新 Job 上下文

**Redis 数据结构**:
```
Key: ojp:cdc:status:{jobId}
Value: {
  "jobId": "job_123",
  "phase": "INCREMENTAL",
  "status": "SNAPSHOT_DONE",
  "timestamp": 1704067200000
}
```

### CHECKPOINT_COMPLETED

**触发时机**: Checkpoint 完成

**行为**:
- 更新心跳时间戳
- 发送 `CHECKPOINT_OK` 事件到 Redis
- 确认 CDC 任务正常运行

**Redis 数据结构**:
```
Key: ojp:cdc:heartbeat:{jobId}
Value: {
  "jobId": "job_123",
  "lastCheckpoint": 1704067200000,
  "status": "RUNNING"
}
```

## ⚙️ 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `OJP_REDIS_HOST` | Redis 主机 | `redis` |
| `OJP_REDIS_PORT` | Redis 端口 | `6379` |
| `OJP_REDIS_AUTH` | Redis 密码 | `""` |
| `OJP_SEATUNNEL_API_URL` | SeaTunnel REST API | `http://seatunnel-master:8080` |

### SeaTunnel 配置

在 SeaTunnel 的 `seatunnel.yaml` 中配置事件处理器：

```yaml
env:
  job.mode: "BATCH"
  checkpoint.interval: 5000

source:
  MySQL-CDC:
    username: root
    password: "123456"
    database-name: "mydb"
    table-name: "users"

sink:
  Print:
    pass_through: false

event-handler:
  - plugin-name: "ojp-cdc-monitor"
    plugin-class: "org.openjdbcproxy.cdc.monitor.OJPCDCEventHandler"
```

## 🚢 部署指南

### 本地部署

#### 1. 构建 JAR

```bash
cd ojp-cdc-monitor
mvn clean package
```

#### 2. 复制到 SeaTunnel

将生成的 JAR 文件复制到 SeaTunnel Master 的 `lib` 目录：

```bash
cp target/ojp-cdc-monitor-0.0.8-alpha.jar $SEATUNNEL_HOME/lib/
```

#### 3. 重启 SeaTunnel

```bash
$SEATUNNEL_HOME/bin/seatunnel-cluster.sh restart
```

### Docker 部署

在 Docker Compose 中配置：

```yaml
version: '3.8'

services:
  seatunnel-master:
    image: apache/seatunnel:2.3.3
    container_name: seatunnel-master
    ports:
      - "8080:8080"
    volumes:
      - ./ojp-cdc-monitor-0.0.8-alpha.jar:/seatunnel/lib/ojp-cdc-monitor-0.0.8-alpha.jar
    environment:
      - OJP_REDIS_HOST=redis
      - OJP_REDIS_PORT=6379
      - OJP_SEATUNNEL_API_URL=http://seatunnel-master:8080
    depends_on:
      - redis

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
```

### Kubernetes 部署

使用 ConfigMap 和 Secret 配置：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ojp-cdc-monitor-config
data:
  OJP_REDIS_HOST: "redis"
  OJP_REDIS_PORT: "6379"
  OJP_SEATUNNEL_API_URL: "http://seatunnel-master:8080"
---
apiVersion: v1
kind: Secret
metadata:
  name: ojp-cdc-monitor-secret
type: Opaque
stringData:
  OJP_REDIS_AUTH: ""
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: seatunnel-master
spec:
  template:
    spec:
      containers:
      - name: seatunnel
        image: apache/seatunnel:2.3.3
        envFrom:
        - configMapRef:
            name: ojp-cdc-monitor-config
        - secretRef:
            name: ojp-cdc-monitor-secret
        volumeMounts:
        - name: ojp-cdc-monitor
          mountPath: /seatunnel/lib/ojp-cdc-monitor-0.0.8-alpha.jar
          subPath: ojp-cdc-monitor-0.0.8-alpha.jar
      volumes:
      - name: ojp-cdc-monitor
        configMap:
          name: ojp-cdc-monitor-jar
```

## 🔧 Job 上下文管理

### 首次事件处理

当接收到一个 Job 的事件时，会通过 REST API 获取 Job 详情：

```http
GET http://seatunnel-master:8080/job-info/{jobId}
```

**响应示例**:
```json
{
  "jobId": "job_123",
  "jobName": "mysql-to-starrocks",
  "env": {
    "job.mode": "BATCH"
  },
  "sources": [
    {
      "pluginName": "MySQL-CDC",
      "config": {
        "database-name": "mydb",
        "table-name": "users"
      }
    }
  ],
  "sinks": [
    {
      "pluginName": "StarRocks",
      "config": {
        "database-name": "starrocks_db",
        "table-name": "users"
      }
    }
  ]
}
```

### 上下文缓存

Job 上下文会被缓存，避免重复查询：

```
Key: ojp:cdc:context:{jobId}
Value: {
  "jobId": "job_123",
  "database": "mydb",
  "table": "users",
  "connHash": "hash_value",
  "createdAt": 1704067200000
}
```

## 📊 监控指标

### Redis 数据结构

| Key 类型 | 说明 | TTL |
|---------|------|-----|
| `ojp:cdc:status:{jobId}` | CDC 状态 | 24h |
| `ojp:cdc:heartbeat:{jobId}` | 心跳数据 | 1h |
| `ojp:cdc:context:{jobId}` | Job 上下文 | 24h |

### 监控端点

```bash
# 获取 CDC 状态
redis-cli GET ojp:cdc:status:job_123

# 获取心跳数据
redis-cli GET ojp:cdc:heartbeat:job_123

# 获取 Job 上下文
redis-cli GET ojp:cdc:context:job_123
```

## 🔍 故障排查

### 常见问题

#### 1. 插件未加载

**问题**: SeaTunnel 启动时未加载插件

**解决方法**:
- 确认 JAR 文件在 `lib` 目录
- 检查 JAR 文件权限
- 查看 SeaTunnel 日志

#### 2. Redis 连接失败

**问题**: 无法连接到 Redis

**解决方法**:
- 检查 Redis 是否运行
- 验证环境变量配置
- 检查网络连接

#### 3. Job 详情获取失败

**问题**: 无法通过 REST API 获取 Job 详情

**解决方法**:
- 检查 SeaTunnel API 地址
- 验证 API 端口是否开放
- 查看 SeaTunnel 日志

#### 4. 事件未触发

**问题**: 事件处理器未收到事件

**解决方法**:
- 检查 SeaTunnel 配置
- 验证事件处理器配置
- 查看 SeaTunnel 日志

## 📚 相关文档

- [主项目 README](../README.md)
- [开发指南](../AGENTS.md)
- [ojp-cache 文档](../ojp-cache/README.md)
- [SeaTunnel 文档](https://seatunnel.apache.org/)

## 🤝 贡献指南

欢迎贡献代码和改进建议！

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。

---

**OJP CDC Monitor** - 让 CDC 状态实时可见，缓存决策更智能！
