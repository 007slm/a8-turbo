# Redis 数据结构设计文档

## 概述

本文档详细描述了 OJP 项目中 Redis 数据存储的格式规范，包括表同步状态、查询统计和CDC监控信息的存储结构。

## 1. 系统架构说明

### 1.1 数据流架构
本系统采用基于CDC同步的状态驱动缓存架构：
- **Redis层**：存储表同步状态、查询统计和CDC监控信息
- **StarRocks层**：作为缓存存储，通过CDC实时同步MySQL数据
- **MySQL层**：源数据库，处理事务性查询
- **缓存决策**：基于表同步状态决定查询路由

### 1.2 数据流向
1. **查询请求** → OJP JDBC Driver
2. **表同步检查** → 查询Redis中表同步状态
3. **路由决策** → 根据同步状态决定查询StarRocks或MySQL
4. **结果返回** → 合并并返回查询结果
5. **统计更新** → 记录查询性能和路由统计

## 2. Redis 键命名规范

### 2.1 基础配置
- **应用名称前缀**: `ojp`
- **键分隔符**: `:`
- **命名模式**: `{prefix}:{module}:{type}:{identifier}`

### 2.2 键命名模式
**分组策略**：所有数据按 `{connHash}` 进行自然分组，代表不同的数据库连接：
- **表同步状态**: `ojp:cdc:sync:state:{connHash}:{tableName}`
- **查询统计**: `ojp:cache:query:{connHash}:{queryId}`
- **慢查询列表**: `ojp:cache:slow:{connHash}`
- **CDC事件**: `ojp:cdc:events:{connHash}`

### 2.3 键命名示例

以下示例展示了基于 `datasourceName` 的自然分组机制：

```
# 电商系统实际键名示例（假设datasourceName为"ecommerce_db"）
fluxdb:cache:rules:ecommerce_db                    # 缓存规则配置
ecommerce_db:cache:config                          # 配置流
ecommerce_db:cache:query:user_profile_by_id        # 用户查询信息
ecommerce_db:cache:query:product_search            # 商品搜索查询信息
ecommerce_db:cache:query:order_history             # 订单历史查询信息
ecommerce_db:cache:user_profile_by_id              # 用户资料缓存（简单查询）
ecommerce_db:cache:product_search:electronics_page1 # 商品搜索缓存（带参数）
ecommerce_db:cache:order_history:user_12345        # 订单历史缓存（带用户ID）
ecommerce_db:cache:config:ack                      # 配置确认
ecommerce_db-cache-query-idx                       # 查询索引
```

## 3. 表同步状态数据结构

### 3.1 同步状态存储

**存储位置**: `ojp:cdc:sync:state:{connHash}:{tableName}`

**存储格式**: Redis String

**数据结构**:
```json
"READY"  // 表已同步完成，可以走缓存
// 或
"SYNCING"  // 表正在同步中，回源查询
```

**状态说明**:
- **READY**: 表数据已通过CDC同步到StarRocks，可以路由到缓存查询
- **SYNCING**: 表数据同步尚未完成，所有涉及此表的查询回源到MySQL
- **状态更新**: 由CDC监控器实时更新，基于SeaTunnel事件

### 3.2 同步状态检查逻辑

```java
public boolean isTableReady(String connHash, String tableName) {
    String key = "ojp:cdc:sync:state:" + connHash + ":" + tableName;
    String state = redisTemplate.opsForValue().get(key);
    return "READY".equals(state);
}
```

## 4. 查询统计数据结构

### 4.1 查询统计存储

**存储位置**: `ojp:cache:query:{connHash}:{queryId}`

**存储格式**: JSON格式

**数据结构**:
```json
{
  "queryId": "user_profile_by_id",
  "connHash": "mysql_localhost_3306_root_ecommerce_db",
  "sql": "SELECT * FROM users WHERE id = ?",
  "tables": ["users"],
  "executionCount": 2847,
  "totalTime": 35000,
  "avgTime": 12.3,
  "maxTime": 45.2,
  "minTime": 3.1,
  "lastExecution": "2024-01-01T12:00:00Z",
  "cacheHits": 2500,
  "cacheMisses": 347,
  "cacheHitRate": 0.878
}
```

### 4.2 慢查询列表存储

**存储位置**: `ojp:cache:slow:{connHash}`

**存储格式**: Redis Sorted Set (按执行时间排序)

**数据结构**:
```
member: queryId
score: avgExecutionTime
```

### 4.3 CDC事件存储

**存储位置**: `ojp:cdc:events:{connHash}`

**存储格式**: Redis Stream

**数据结构**:
```json
{
  "eventType": "SNAPSHOT_COMPLETED",
  "tableName": "users",
  "timestamp": "2024-01-01T12:00:00Z",
  "details": {
    "recordsProcessed": 100000,
    "duration": 300000
  }
}
```

## 5. 缓存决策实现

### 5.1 缓存策略

当前架构采用基于表同步状态的缓存决策策略：

1. **SQL解析**: 提取查询涉及的表名
2. **状态检查**: 检查各表的CDC同步状态
3. **路由决策**: 根据同步状态决定查询StarRocks或MySQL
4. **统计收集**: 记录查询性能和路由统计

### 5.2 决策逻辑

```java
public boolean shouldCache(String connHash, String sql) {
    Set<String> tables = parseTables(sql);
    for (String table : tables) {
        if (!isTableReady(connHash, table)) {
            return false; // 回源查询
        }
    }
    return true; // 走缓存查询
}
```

## 6. 统计信息数据结构

### 5.1 查询统计

**存储位置**: `{datasourceName}:cache:stats:queries`
**数据类型**: Hash
**更新频率**: 实时更新

#### 6.1.1 用户查询统计
```json
{
  "queryId": "user_profile_by_id",
  "count": 2847,
  "mean": 12.3,
  "max": 45.2,
  "min": 3.1,
  "timestamp": 1704067200000,
  "cacheHitRate": 0.89,
  "category": "user_management"
}
```

#### 6.1.2 商品搜索统计
```json
{
  "queryId": "product_search",
  "count": 1523,
  "mean": 45.7,
  "max": 120.8,
  "min": 15.2,
  "timestamp": 1704067140000,
  "cacheHitRate": 0.76,
  "category": "product_catalog"
}
```

#### 6.1.3 订单查询统计
```json
{
  "queryId": "order_history",
  "count": 892,
  "mean": 67.2,
  "max": 180.5,
  "min": 22.1,
  "timestamp": 1704067080000,
  "cacheHitRate": 0.82,
  "category": "order_management"
}
```

#### 6.1.4 购物车查询统计
```json
{
  "queryId": "user_cart_items",
  "count": 1247,
  "mean": 23.8,
  "max": 85.3,
  "min": 8.2,
  "timestamp": 1704067160000,
  "cacheHitRate": 0.91,
  "category": "shopping_cart"
}
```

#### 6.1.5 分类商品查询统计
```json
{
  "queryId": "category_products",
  "count": 2156,
  "mean": 38.4,
  "max": 125.7,
  "min": 12.8,
  "timestamp": 1704067100000,
  "cacheHitRate": 0.73,
  "category": "product_catalog"
}
```

#### 6.1.6 库存检查统计
```json
{
  "queryId": "inventory_check",
  "count": 3421,
  "mean": 15.6,
  "max": 42.1,
  "min": 5.3,
  "timestamp": 1704067220000,
  "cacheHitRate": 0.95,
  "category": "inventory_management"
}
```

### 6.1 统计信息实现说明

统计信息通过以下方式收集：

1. **查询统计**: 实时记录每个查询的执行时间和路由结果
2. **缓存命中率**: 计算缓存查询vs源查询的比例
3. **性能监控**: 跟踪StarRocks和MySQL的响应时间差异
4. **慢查询识别**: 基于执行时间阈值识别性能问题

### 6.2 统计指标

- **决策延迟**: 缓存决策的平均响应时间
- **缓存命中**: 路由到StarRocks的查询次数
- **缓存未命中**: 回源到MySQL的查询次数
- **查询延迟**: 缓存查询vs源查询的性能对比

## 7. 配置管理

### 7.1 CDC状态更新机制

**更新方式**: 由CDC监控器实时推送状态变更

**状态源**: SeaTunnel CDC事件监听

**更新频率**: 实时，基于CDC事件触发

### 7.2 配置参数

**Redis连接配置**:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

**CDC配置**:
```yaml
ojp:
  cdc:
    monitor:
      enabled: true
      redis-stream-key: ojp:cdc:events
```
成员: applicationInstanceId (客户端ID或配置的实例ID)
分数: 配置消息的时间戳分数
```

### 6.2 配置流消息格式

**存储格式**: 文本格式（键值对字符串）

**Stream消息体**:
```json
{
  "rules.1.ttl": "3600",
  "rules.1.tables.1": "users",
  "rules.1.tables.2": "orders",
  "rules.2.ttl": "1800",
  "rules.2.tables-any.1": "products"
}
```

## 8. API 数据格式

### 8.1 查询统计API

**获取查询统计**: `GET /api/cache/queries/list`

**响应格式**:
```json
{
  "mysql_localhost_3306_root_ecommerce_db": [
    {
      "queryId": "user_profile_by_id",
      "sql": "SELECT * FROM users WHERE id = ?",
      "tables": ["users"],
      "executionCount": 2847,
      "avgTime": 12.3,
      "cacheHitRate": 0.89,
      "lastExecution": "2024-01-01T12:00:00Z"
    }
  ]
}
```

### 8.2 表同步状态API

**获取表状态**: `GET /api/cache/tables/status`

**响应格式**:
```json
{
  "mysql_localhost_3306_root_ecommerce_db": {
    "users": "READY",
    "orders": "SYNCING",
    "products": "READY"
  }
}
```

### 8.3 CDC事件API

**获取CDC事件**: `GET /api/cdc/events`

**响应格式**:
```json
{
  "events": [
    {
      "eventType": "SNAPSHOT_COMPLETED",
      "tableName": "users",
      "timestamp": "2024-01-01T12:00:00Z",
      "recordsProcessed": 100000
    }
  ]
}
```