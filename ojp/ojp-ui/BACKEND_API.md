# OJP Server Management Console - 后端 API 文档

本文档描述了 OJP Server Management Console 前端应用所需的后端 API 接口。后端开发人员需要根据这些接口规范实现相应的功能。

## 基础信息

- **基础路径**: `/api`
- **内容类型**: `application/json`
- **认证方式**: 待定（建议使用 JWT Token）
- **响应格式**: 统一 JSON 格式

## 通用响应格式

```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

错误响应格式：
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## 1. 系统状态相关接口

### 1.1 获取系统健康状态
- **路径**: `GET /api/actuator/health`
- **描述**: 获取系统健康状态
- **响应示例**:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  },
  "uptime": 3600,
  "version": "1.0.0",
  "profiles": ["dev"]
}
```

### 1.2 获取系统信息
- **路径**: `GET /api/actuator/info`
- **描述**: 获取系统基本信息

### 1.3 获取系统指标
- **路径**: `GET /api/actuator/metrics`
- **描述**: 获取系统指标列表

### 1.4 获取特定指标
- **路径**: `GET /api/actuator/metrics/{metricName}`
- **描述**: 获取特定指标的值

## 2. 服务器管理相关接口

❌ 已移除 - 后台管理系统不需要服务器管理功能

## 3. 缓存管理相关接口

### 3.1 缓存概览统计
- **路径**: `GET /api/cache/stats/overview`
- **描述**: 获取缓存系统概览统计
- **响应示例**:
```json
{
  "totalCaches": 10,
  "activeCaches": 8,
  "totalKeys": 1500,
  "memoryUsage": 75.5,
  "hitRate": 85.2,
  "avgResponseTime": 12.5
}
```

### 3.2 缓存命中率统计
- **路径**: `GET /api/cache/stats/hit-rate?timeRange={timeRange}`
- **描述**: 获取缓存命中率统计
- **参数**: timeRange (1h, 6h, 24h, 7d, 30d)
- **响应示例**:
```json
{
  "currentRate": 85.2,
  "averageRate": 82.1,
  "maxRate": 90.5,
  "trend": "up"
}
```

### 3.3 查询性能统计
- **路径**: `GET /api/cache/stats/query-performance?timeRange={timeRange}`
- **描述**: 获取查询性能统计
- **响应示例**:
```json
{
  "avgQueryTime": 45.2,
  "avgCachedQueryTime": 12.5,
  "avgNonCachedQueryTime": 156.8,
  "performanceImprovement": 72.1
}
```

### 3.4 热门表格统计
- **路径**: `GET /api/cache/stats/top-tables`
- **描述**: 获取热门表格访问统计
- **响应示例**:
```json
[
  {
    "name": "users",
    "accessFrequency": 2500,
    "avgQueryTime": 156.7,
    "cached": true
  }
]
```

### 3.5 慢查询统计
- **路径**: `GET /api/cache/stats/slow-queries`
- **描述**: 获取慢查询统计
- **响应示例**:
```json
[
  {
    "sql": "SELECT * FROM users WHERE complex_condition...",
    "executionTime": 2500,
    "count": 15
  }
]
```

### 3.6 缓存规则管理

#### 3.6.1 获取缓存规则列表
- **路径**: `GET /api/cache/rules`
- **描述**: 获取所有缓存规则
- **响应示例**:
```json
[
  {
    "id": "rule-1",
    "name": "用户表缓存规则",
    "ruleType": "TABLES",
    "ruleMatch": "users",
    "ttl": "30m",
    "status": "ACTIVE",
    "lastUpdated": "2024-01-01T00:00:00Z",
    "isDefault": false
  }
]
```

#### 3.6.2 创建缓存规则
- **路径**: `POST /api/cache/rules`
- **描述**: 创建新的缓存规则
- **请求体**:
```json
{
  "name": "规则名称",
  "ruleType": "TABLES",
  "ruleMatch": "users,orders",
  "ttl": "30m",
  "description": "规则描述",
  "status": "ACTIVE"
}
```

#### 3.6.3 更新缓存规则
- **路径**: `PUT /api/cache/rules/{id}`
- **描述**: 更新缓存规则

#### 3.6.4 删除缓存规则
- **路径**: `DELETE /api/cache/rules/{id}`
- **描述**: 删除缓存规则

#### 3.6.5 提交缓存规则
- **路径**: `POST /api/cache/rules/commit`
- **描述**: 批量提交缓存规则更改

#### 3.6.6 验证缓存规则
- **路径**: `POST /api/cache/rules/validate`
- **描述**: 验证缓存规则的有效性

### 3.7 查询缓存管理

#### 3.7.1 获取查询列表
- **路径**: `GET /api/cache/queries?search={search}&field={field}&direction={direction}`
- **描述**: 获取查询列表，支持搜索和排序
- **响应示例**:
```json
[
  {
    "queryId": "q1",
    "sql": "SELECT * FROM users WHERE id = ?",
    "tables": ["users"],
    "count": 1500,
    "meanQueryTime": 120.5,
    "isCached": true,
    "currentTtl": "30m"
  }
]
```

#### 3.7.2 为查询创建缓存规则
- **路径**: `POST /api/cache/queries/create-rule`
- **描述**: 为特定查询创建缓存规则

### 3.8 表格缓存管理

#### 3.8.1 获取表格列表
- **路径**: `GET /api/cache/tables?search={search}`
- **描述**: 获取表格列表
- **响应示例**:
```json
[
  {
    "name": "users",
    "ttl": "30m",
    "avgQueryTime": 156.7,
    "accessFrequency": 2500,
    "cached": true
  }
]
```

#### 3.8.2 为表格创建缓存规则
- **路径**: `POST /api/cache/tables/create-rule`
- **描述**: 为特定表格创建缓存规则

#### 3.8.3 获取表格统计
- **路径**: `GET /api/cache/tables/{tableName}/stats`
- **描述**: 获取特定表格的统计信息

## 4. 系统监控

OJP Server 集成了 Spring Boot Actuator，前端可以直接调用标准的 Actuator 端点：

### 4.1 系统健康检查
- **路径**: `GET /actuator/health`
- **描述**: 获取系统健康状态

### 4.2 系统信息
- **路径**: `GET /actuator/info`
- **描述**: 获取系统基本信息

### 4.3 系统指标
- **路径**: `GET /actuator/metrics`
- **描述**: 获取所有可用指标列表

### 4.4 特定指标
- **路径**: `GET /actuator/metrics/{metricName}`
- **描述**: 获取特定指标的值

### 4.5 环境信息
- **路径**: `GET /actuator/env`
- **描述**: 获取环境配置信息

### 4.6 配置属性
- **路径**: `GET /actuator/configprops`
- **描述**: 获取配置属性信息

### 4.7 Bean 信息
- **路径**: `GET /actuator/beans`
- **描述**: 获取 Spring Bean 信息

### 4.8 线程转储
- **路径**: `GET /actuator/threaddump`
- **描述**: 获取线程转储信息

**注意**：这些端点由 Spring Boot Actuator 提供，无需额外的后端实现。前端可以直接调用这些标准的 Actuator 端点。

## 5. 日志相关接口

### 5.1 获取应用日志
- **路径**: `GET /api/logs/application`
- **描述**: 获取应用日志

### 5.2 获取访问日志
- **路径**: `GET /api/logs/access`
- **描述**: 获取访问日志

### 5.3 获取错误日志
- **路径**: `GET /api/logs/error`
- **描述**: 获取错误日志

## 6. SQL统计数据相关接口

### 6.1 获取统计概览
- **路径**: `GET /api/statistics/overview`
- **描述**: 获取SQL统计概览信息

### 6.2 获取所有SQL统计数据
- **路径**: `GET /api/statistics/sql`
- **描述**: 获取所有SQL统计数据

### 6.3 获取指定SQL统计数据
- **路径**: `GET /api/statistics/sql/{queryId}`
- **描述**: 获取指定SQL查询的统计数据

### 6.4 获取热门SQL查询
- **路径**: `GET /api/statistics/sql/hot?limit=10`
- **描述**: 获取热门SQL查询（按执行次数排序）

### 6.5 获取慢查询
- **路径**: `GET /api/statistics/sql/slow?limit=10`
- **描述**: 获取慢查询（按平均执行时间排序）

### 6.6 获取所有表统计数据
- **路径**: `GET /api/statistics/tables`
- **描述**: 获取所有表统计数据

### 6.7 获取指定表统计数据
- **路径**: `GET /api/statistics/tables/{tableName}`
- **描述**: 获取指定表的统计数据

### 6.8 获取热门表
- **路径**: `GET /api/statistics/tables/hot?limit=10`
- **描述**: 获取热门表（按访问频率排序）

### 6.9 获取缓存命中率统计
- **路径**: `GET /api/statistics/cache/hit-rate`
- **描述**: 获取缓存命中率统计信息

### 6.10 清理过期统计数据
- **路径**: `POST /api/statistics/cleanup`
- **描述**: 清理过期的统计数据

## 7. 系统设置相关接口

❌ 已移除 - 后台管理系统不需要系统设置功能

## 8. 用户管理相关接口

❌ 已移除 - 后台管理系统不需要用户概念

## 实现优先级

### 高优先级（必须实现）
1. 系统健康状态接口 (`/actuator/health`)
2. 缓存概览统计接口
3. 缓存规则管理接口
4. SQL统计数据接口

### 中优先级（建议实现）
1. 查询缓存管理接口
2. 表格缓存管理接口
3. 日志管理接口
4. 统计数据分析接口

### 低优先级（可选实现）
1. 高级统计功能
2. 统计报表导出功能

## 技术建议

1. **使用 Spring Boot**: 推荐使用 Spring Boot 框架实现后端
2. **缓存实现**: 建议使用 Redis 作为缓存后端
3. **数据库**: 推荐使用 MySQL 或 PostgreSQL 存储配置和统计数据
4. **监控**: 集成 Spring Boot Actuator 提供系统监控
5. **安全**: 实现 JWT 认证和权限控制
6. **日志**: 使用 SLF4J + Logback 进行日志管理
7. **API 文档**: 使用 Swagger/OpenAPI 生成 API 文档

## 测试建议

1. **单元测试**: 为每个服务层方法编写单元测试
2. **集成测试**: 测试 API 接口的完整流程
3. **性能测试**: 测试缓存系统的性能表现
4. **压力测试**: 测试系统在高并发下的表现

## 部署建议

1. **容器化**: 使用 Docker 容器化部署
2. **配置管理**: 使用配置文件或环境变量管理配置
3. **监控告警**: 集成 Prometheus + Grafana 进行监控
4. **日志收集**: 使用 ELK Stack 进行日志收集和分析

---

**注意**: 本文档中的接口规范是前端开发的基础，后端开发人员可以根据实际需求进行调整和扩展。建议在开发过程中保持前后端的及时沟通，确保接口的一致性和完整性。
