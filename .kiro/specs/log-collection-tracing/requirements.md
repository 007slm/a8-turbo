# 需求文档

## 介绍

为 OJP (Open JDBC Proxy) 项目构建一个轻量级的日志收集和链路追踪系统，解决当前日志分散在各个容器中难以排查问题的痛点。

## 术语表

- **OJP**: Open JDBC Proxy，智能数据库代理中间件
- **TraceID**: 链路追踪标识符，用于关联同一请求的所有日志
- **Vector**: 轻量级日志收集器
- **Loki**: 轻量级日志存储和查询引擎
- **gRPC**: Google Remote Procedure Call，OJP 核心通信协议
- **MDC**: Mapped Diagnostic Context，日志上下文映射

## 需求

### 需求 1: 日志集中收集

**用户故事:** 作为运维人员，我希望能够在一个地方查看所有 OJP 相关服务的日志，而不需要登录到各个容器中查看。

#### 验收标准

1. WHEN 系统部署完成 THEN 系统 SHALL 自动收集所有 OJP 相关容器的日志
2. WHEN 日志收集器启动 THEN 系统 SHALL 监控 ojp-server、shopservice 等核心服务容器
3. WHEN 容器重启或新增 THEN 日志收集器 SHALL 自动发现并开始收集新容器的日志
4. WHEN 日志量过大 THEN 系统 SHALL 自动轮转和清理超过保留期的日志
5. WHEN 日志收集器异常 THEN 系统 SHALL 记录错误并尝试自动恢复

### 需求 2: 无侵入链路追踪

**用户故事:** 作为开发人员，我希望能够通过 TraceID 追踪一个 JDBC 请求在整个系统中的执行路径，而不需要修改现有的业务代码。

#### 验收标准

1. WHEN JDBC 客户端发起请求 THEN ojp-jdbc-driver SHALL 自动生成 TraceID
2. WHEN gRPC 请求发送到 ojp-server THEN TraceID SHALL 通过 gRPC metadata 传递
3. WHEN ojp-server 处理请求 THEN 所有相关日志 SHALL 包含相同的 TraceID
4. WHEN 请求路由到 MySQL 或 StarRocks THEN 数据库操作日志 SHALL 包含 TraceID
5. WHEN 系统出现异常 THEN 错误日志 SHALL 包含 TraceID 便于问题定位

### 需求 3: 轻量级存储和查询

**用户故事:** 作为系统管理员，我希望日志系统占用最少的资源，同时提供快速的日志查询能力。

#### 验收标准

1. WHEN 系统运行 THEN 日志存储组件 SHALL 占用内存不超过 512MB
2. WHEN 查询最近 1 小时的日志 THEN 系统 SHALL 在 3 秒内返回结果
3. WHEN 存储空间不足 THEN 系统 SHALL 自动删除超过 7 天的历史日志
4. WHEN 并发查询请求 THEN 系统 SHALL 支持至少 10 个并发查询
5. WHEN 日志写入速度过快 THEN 系统 SHALL 通过缓冲机制保证不丢失日志

### 需求 4: 集成化用户界面

**用户故事:** 作为运维人员，我希望在 OJP 管理界面中直接查看和搜索日志，而不需要切换到其他系统。

#### 验收标准

1. WHEN 访问 OJP 管理界面 THEN 系统 SHALL 提供日志查询入口
2. WHEN 输入 TraceID THEN 系统 SHALL 显示该链路的所有相关日志
3. WHEN 选择时间范围 THEN 系统 SHALL 显示该时间段内的日志
4. WHEN 日志内容过长 THEN 界面 SHALL 提供展开/折叠功能
5. WHEN 发现异常日志 THEN 界面 SHALL 高亮显示错误级别的日志

### 需求 5: 问题排查支持

**用户故事:** 作为技术支持人员，我希望能够快速定位和分析系统问题，特别是性能问题和错误问题。

#### 验收标准

1. WHEN 系统出现慢查询 THEN 日志 SHALL 记录查询执行时间和 SQL 语句
2. WHEN 缓存决策发生 THEN 日志 SHALL 记录路由决策过程和原因
3. WHEN gRPC 通信异常 THEN 日志 SHALL 记录详细的错误信息和堆栈
4. WHEN 数据库连接异常 THEN 日志 SHALL 记录连接池状态和错误详情
5. WHEN CDC 同步异常 THEN 日志 SHALL 记录 SeaTunnel 作业状态和错误信息

### 需求 6: 运维友好特性

**用户故事:** 作为运维人员，我希望日志系统易于部署、配置和维护。

#### 验收标准

1. WHEN 部署日志系统 THEN 系统 SHALL 通过 Docker Compose 一键启动
2. WHEN 修改日志配置 THEN 系统 SHALL 支持热重载配置文件
3. WHEN 系统资源紧张 THEN 日志收集器 SHALL 自动降低采集频率
4. WHEN 存储空间告警 THEN 系统 SHALL 发送通知并自动清理旧日志
5. WHEN 需要备份日志 THEN 系统 SHALL 提供日志导出功能

### 需求 7: 日志规范化和中文化

**用户故事:** 作为开发和运维人员，我希望系统日志格式统一、内容清晰，使用中文描述便于理解和排查问题。

#### 验收标准

1. WHEN 系统输出日志 THEN 所有日志 SHALL 包含统一的 SessionUUID 格式标识
2. WHEN 记录会话操作 THEN 日志 SHALL 使用中文描述操作类型和状态
3. WHEN 记录数据库操作 THEN 日志 SHALL 包含操作类型、目标数据库和执行时间
4. WHEN 记录缓存决策 THEN 日志 SHALL 包含决策结果、表名和决策原因
5. WHEN 记录异常信息 THEN 日志 SHALL 包含 SessionUUID、操作描述和详细错误信息