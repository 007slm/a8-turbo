# OJP 日志收集和链路追踪系统

## 概述

OJP 日志收集和链路追踪系统基于 Vector + Loki 技术栈，提供：

- **无侵入链路追踪**：基于现有 SessionUUID 机制
- **集中日志收集**：自动收集所有 OJP 相关容器日志
- **智能日志解析**：支持中文日志内容和多维度分类
- **集成查询界面**：直接在 OJP-UI 中查询和分析日志

## 系统架构

```
业务应用 → ojp-jdbc-driver → ojp-server → 容器日志
                                    ↓
                               Vector 收集器
                                    ↓
                               Loki 存储
                                    ↓
                            OJP-UI 日志查询界面
```

## 部署说明

### 1. 自动部署

日志系统已集成到主 Docker Compose 体系中，无需单独部署：

```bash
# 启动完整 OJP 系统（包含日志收集）
./start-dev.bat

# 或者使用 Docker Compose
docker-compose up -d
```

### 2. 服务组件

- **Loki** (172.24.0.50:3100): 日志存储和查询引擎
- **Vector** (172.24.0.51): 日志收集器
- **Kong 网关**: 提供 `/api/loki` 路由访问 Loki API

### 3. 访问方式

- **主要入口**: OJP-UI 日志查询页面 (`http://localhost:8000/logs`)
- **直接访问**: Loki API (`http://localhost:8000/api/loki`)

## 使用指南

### 1. 日志查询

在 OJP-UI 中访问"日志查询"菜单：

1. **SessionUUID 查询**: 输入会话 UUID 查看完整链路日志
2. **时间范围查询**: 选择时间范围查看历史日志
3. **多维度过滤**: 按服务、级别、容器等维度过滤
4. **关键词搜索**: 在日志内容中搜索特定关键词

### 2. 链路追踪

系统自动基于 SessionUUID 进行链路追踪：

- 每个 JDBC 连接对应一个唯一的 SessionUUID
- 所有相关日志都包含相同的 SessionUUID
- 点击日志中的 SessionUUID 标签可查看完整链路

### 3. 日志分类

系统自动识别和分类不同类型的日志：

- **会话操作**: 创建会话、终止会话等
- **数据库操作**: 查询执行、连接管理等
- **缓存决策**: 缓存命中、未命中、路由决策等
- **慢查询**: 执行时间超过阈值的查询
- **异常日志**: 系统错误和异常情况

## 配置说明

### 1. Vector 配置

位置: `docker/vector/vector.toml`

主要配置项：
- 监控容器: `ojp-server`, `shopservice`, `ojp-ui`
- SessionUUID 提取规则: 支持中文和英文格式
- 日志分类标记: 自动识别不同类型的操作

### 2. Loki 配置

位置: `docker/loki/loki-config.yaml`

主要配置项：
- 日志保留期: 7天
- 内存限制: 512MB
- 查询性能: 3秒内响应
- 并发支持: 10个并发查询

### 3. Kong 路由配置

位置: `docker/kong/kong.yml`

Loki 服务路由：
```yaml
- name: loki
  url: http://loki:3100
  routes:
  - name: loki-api
    paths:
    - /api/loki
```

## 开发指南

### 1. 使用 OjpLogger

在 Java 代码中使用统一的日志工具类：

```java
import org.openjdbcproxy.common.logging.OjpLogger;

// 记录会话操作
OjpLogger.logSession(log, sessionUUID, "创建会话", "客户端:" + clientUUID);

// 记录数据库操作
OjpLogger.logDatabase(log, sessionUUID, "查询", "MySQL", 150);

// 记录缓存决策
OjpLogger.logCacheDecision(log, sessionUUID, "命中", "user_table", "热点数据");

// 记录慢查询
OjpLogger.logSlowQuery(log, sessionUUID, sql, 2000);

// 记录异常
OjpLogger.logError(log, sessionUUID, "数据库连接", exception);
```

### 2. 日志格式规范

所有日志都遵循统一格式：
- 包含 SessionUUID 标识: `会话[uuid]`
- 使用中文描述操作类型
- 包含必要的上下文信息（执行时间、目标等）

### 3. 前端集成

在 React 组件中使用 Loki API：

```javascript
import { lokiApi, LogQLBuilder } from '../services/lokiApi'

// 查询特定会话的日志
const query = LogQLBuilder.buildSessionQuery(sessionUUID)
const logs = await lokiApi.queryRange(query, startTime, endTime)

// 查询慢查询日志
const slowQuery = LogQLBuilder.buildSlowQueryFilter({ service: 'ojp-server' })
const slowLogs = await lokiApi.queryRange(slowQuery, startTime, endTime)
```

## 监控和维护

### 1. 健康检查

系统提供多层健康检查：
- Vector: 配置文件验证
- Loki: HTTP 就绪检查
- 前端: 自动检测服务可用性

### 2. 存储管理

- 自动清理超过 7 天的历史日志
- 压缩存储减少磁盘占用
- 监控存储空间使用情况

### 3. 性能优化

- 批量处理日志写入
- 索引优化提高查询速度
- 缓存查询结果

## 故障排查

### 1. 常见问题

**日志服务不可用**:
- 检查 Loki 和 Vector 容器状态
- 验证网络连接和端口配置
- 查看容器日志排查启动问题

**日志查询缓慢**:
- 缩小时间范围
- 使用更具体的过滤条件
- 检查 Loki 资源使用情况

**SessionUUID 提取失败**:
- 检查日志格式是否符合规范
- 验证 Vector 解析规则配置
- 确认 OjpLogger 使用正确

### 2. 调试工具

- Vector API: `http://localhost:8686` (如果直接暴露)
- Loki API: `http://localhost:8000/api/loki`
- 容器日志: `docker-compose logs vector loki`

## 扩展和定制

### 1. 添加新的日志源

在 `docker/vector/vector.toml` 中添加新容器：

```toml
[sources.docker_logs]
include_containers = ["ojp-server", "shopservice", "ojp-ui", "new-service"]
```

### 2. 自定义日志解析规则

在 Vector 配置中添加新的解析规则：

```toml
# 检测新的操作类型
if match(.content, r"新操作|new_operation") {
  .is_new_operation = true
}
```

### 3. 扩展前端查询功能

在 `LogExplorer.jsx` 中添加新的过滤条件和查询类型。

## 版本信息

- Vector: 0.34.0-alpine
- Loki: 2.9.0
- 集成版本: OJP 0.0.8-alpha