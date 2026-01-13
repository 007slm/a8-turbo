# OJP CDC Monitor 修复日志

## 修复的问题

### 1. Type 字段逻辑问题
**问题描述**: 
- 原代码中向 Redis Stream 写入的 `type` 字段是硬编码的，不够灵活
- `handleReaderClose` 方法固定使用 `"SNAPSHOT_DONE"`
- `handleCheckpoint` 方法固定使用 `"BATCH_CHECKPOINT"`

**修复方案**:
- 新增 `determineEventType()` 方法，根据原始事件类型动态映射
- 保留原始事件类型信息在 `originalEventType` 字段中
- 支持更精确的事件类型分类：
  - `LIFECYCLE_READER_CLOSE` → `LIFECYCLE_SNAPSHOT_DONE`
  - `READER_CLOSE` → `READER_SNAPSHOT_DONE`
  - `ENUMERATOR_CLOSE` → `ENUMERATOR_SNAPSHOT_DONE`

### 2. 缓冲区刷新问题
**问题描述**:
- 检查点缓冲区只在新事件到达时才刷新，可能导致数据丢失
- 没有大小限制，可能导致内存问题

**修复方案**:
- 新增定时刷新机制 `periodicFlush()`，每 3 秒自动检查并刷新
- 增加缓冲区大小限制（50条记录），达到限制时立即刷新
- 改进刷新触发条件：时间间隔 OR 大小限制

### 3. 日志系统改进
**问题描述**:
- 原代码使用手写的文件操作进行日志记录
- 没有使用标准的日志库配置
- 日志格式不统一，难以管理

**修复方案**:
- 移除手写的日志文件操作代码
- 使用标准的 SLF4J + Logback 配置
- 创建专用的事件日志记录器 `CDC_EVENTS`
- 配置日志文件滚动策略，避免文件过大
- 结构化日志格式，便于问题追踪

## 日志配置

### 日志文件位置
- 主日志文件: `~/ojp-cdc-logs/ojp-cdc-monitor.log`
- 事件日志文件: `~/ojp-cdc-logs/ojp-cdc-monitor-events.log`

### 日志滚动策略
- 主日志: 每个文件最大 10MB，保留 30 天，总大小限制 300MB
- 事件日志: 每个文件最大 50MB，保留 7 天，总大小限制 500MB

### 关键日志事件
- `EVENT_RECEIVED`: 接收到 SeaTunnel 事件
- `READER_CLOSE_PROCESSING`: 处理 Reader Close 事件
- `SNAPSHOT_COMPLETED`: 快照完成
- `CHECKPOINT_PROCESSING`: 处理检查点事件
- `CHECKPOINT_BUFFERED`: 检查点已缓冲
- `BATCH_CHECKPOINT_FLUSHED`: 批次检查点已刷新
- `JOB_CONTEXT_FETCHED`: 作业上下文获取成功
- `UNHANDLED_EVENT`: 未处理的事件类型

## 使用建议

1. **监控日志文件**: 定期检查 `~/ojp-cdc-logs/` 目录下的日志文件
2. **问题排查**: 使用事件日志文件快速定位 CDC 相关问题
3. **性能监控**: 关注缓冲区刷新频率和大小
4. **配置调优**: 根据实际情况调整 `FLUSH_INTERVAL_MS` 和缓冲区大小限制

## 环境变量

确保以下环境变量正确配置：
- `OJP_REDIS_HOST`: Redis 主机地址
- `OJP_REDIS_PORT`: Redis 端口
- `OJP_REDIS_AUTH`: Redis 认证密码（可选）
- `OJP_SEATUNNEL_API_URL`: SeaTunnel API 地址