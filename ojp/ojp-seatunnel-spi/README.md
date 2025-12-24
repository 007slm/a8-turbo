# ojp-seatunnel-spi 模块

SeaTunnel Event Handler SPI 插件，用于实现 `ojp-cache` 的实时 CDC 状态监控。

## 功能

监听 SeaTunnel 引擎事件，将 CDC 状态推送至 Redis，供 `ojp-cache` 进行缓存决策。

### 监听事件

| 事件 | 触发时机 | 行为 |
|:---|:---|:---|
| `LIFECYCLE_READER_CLOSE` | Snapshot 完成 | 标记 `phase=INCREMENTAL`，发送 `SNAPSHOT_DONE` |
| `CHECKPOINT_COMPLETED` | Checkpoint 完成 | 更新心跳，发送 `CHECKPOINT_OK` |

### Job 上下文获取

首次收到某 Job 的事件时，通过 **REST API** 查询 Job 详情获取 `database/table/connHash`，并缓存供后续使用（Job 配置不变）。

```
GET http://seatunnel-master:8080/job-info/{jobId}
```

## 环境变量

| 变量 | 说明 | 默认值 |
|:---|:---|:---|
| `OJP_REDIS_HOST` | Redis 地址 | `redis` |
| `OJP_REDIS_PORT` | Redis 端口 | `6379` |
| `OJP_REDIS_AUTH` | Redis 密码 | 空 |
| `OJP_SEATUNNEL_API_URL` | SeaTunnel REST API 地址 | `http://seatunnel-master:8080` |

## 部署

将打包后的 JAR 放置在 SeaTunnel Master 的 `lib` 目录下。
