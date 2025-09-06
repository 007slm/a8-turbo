# Redis 数据结构设计文档

## 概述

本文档详细描述了 fluxdb-cache 项目中 Redis 数据存储的格式规范，包括缓存规则、查询缓存、表格缓存和统计信息的存储结构。

## 1. 系统架构说明

### 1.1 数据流架构
本系统采用Redis + StarRocks的双层存储架构：
- **Redis层**：作为配置和统计信息存储层，缓存数据从StarRocks中查询获得
- **StarRocks层**：作为主要数据存储和分析引擎，提供高性能查询能力，性能优于MySQL等传统数据库
- **数据同步**：通过Flink CDC根据用户缓存配置中的相关表从MySQL同步到StarRocks

### 1.2 数据流向
1. **查询请求** → OJP Server（查询处理服务）
2. **缓存决策** → 根据Redis中缓存配置决定是否走缓存
3. **走缓存路径** → 从StarRocks获取数据（高性能OLAP查询）
4. **不走缓存路径** → 从MySQL获取数据（传统关系型数据库查询）
5. **统计信息** → Redis存储（缓存命中率、查询性能等统计）

## 2. Redis 键命名规范

### 2.1 基础配置
- **应用名称前缀**: `fluxdb` 
- **键分隔符**: `:` 
- **属性前缀**: `fluxdb` (固定值)

### 2.2 键命名模式
基于 KeyBuilder 类的实现，所有键都遵循以下模式。**分组策略**：所有数据按 `{datasourceName}` 进行自然分组，Redis 键中包含数据库标识符，无需额外的分组逻辑：
- **配置流键**: `{datasourceName}:cache:config`
- **查询键**: `{datasourceName}:cache:query:{queryId}`
- **缓存规则键**: `fluxdb:cache:rules:{datasourceName}`
- **查询缓存键**: `{datasourceName}:cache:{queryId}` 或 `{datasourceName}:cache:{queryId}:{paramsHash}`
- **查询索引**: `{datasourceName}-cache-query-idx`
- **配置确认键**: `{datasourceName}:cache:config:ack`

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

## 3. 缓存规则数据结构

### 3.1 规则配置存储

**存储位置**: Redis Stream `{datasourceName}:config`

**存储格式**: 文本格式（键值对字符串）

**数据结构**:
```json
{
  "rules.1.ttl": "1800",
  "rules.1.tables.1": "users",
  "rules.1.description": "用户基础信息缓存30分钟",
  "rules.2.ttl": "600",
  "rules.2.tables-any.1": "products",
  "rules.2.description": "商品相关查询缓存10分钟",
  "rules.3.ttl": "300",
  "rules.3.tables.1": "orders",
  "rules.3.description": "订单查询缓存5分钟",
  "rules.4.ttl": "60",
  "rules.4.tables.1": "inventory",
  "rules.4.description": "库存信息缓存1分钟",
  "rules.5.ttl": "0",
  "rules.5.description": "默认不缓存规则"
}
```

**备用存储位置**: Redis String `fluxdb:cache:rules:{datasourceName}`

**存储格式**: JSON格式

**备用数据结构**:
```json
[
  {
    "id": "user_profile_cache",
    "ttl": 1800,
    "tables": ["users"],
    "priority": 1,
    "enabled": true,
    "description": "用户基础信息缓存30分钟"
  },
  {
    "id": "product_search_cache",
    "ttl": 600,
    "tablesAny": ["products"],
    "priority": 2,
    "enabled": true,
    "description": "商品搜索结果缓存10分钟"
  },
  {
    "id": "order_history_cache",
    "ttl": 300,
    "tables": ["orders"],
    "priority": 3,
    "enabled": true,
    "description": "用户订单历史缓存5分钟"
  },
  {
    "id": "inventory_check_cache",
    "ttl": 60,
    "tables": ["inventory"],
    "priority": 4,
    "enabled": true,
    "description": "库存信息缓存1分钟"
  },
  {
    "id": "shopping_cart_cache",
    "ttl": 1800,
    "queryIds": ["user_cart_items"],
    "priority": 5,
    "enabled": true,
    "description": "购物车内容缓存30分钟"
  },
  {
    "id": "category_products_cache",
    "ttl": 3600,
    "regex": "SELECT.*FROM products.*WHERE category_id.*ORDER BY.*",
    "priority": 6,
    "enabled": true,
    "description": "分类商品列表缓存1小时"
  },
  {
    "id": "payment_methods_cache",
    "ttl": 7200,
    "tables": ["payment_methods"],
    "priority": 7,
    "enabled": true,
    "description": "支付方式配置缓存2小时"
  }
]
```

### 3.2 规则类型说明

#### 3.2.1 表格精确匹配 (tables)
- **匹配逻辑**: 查询涉及的表格必须完全匹配规则中指定的表格
- **示例**: 规则指定 `["users"]`，只有查询 `users` 表的SQL会被匹配
- **配置字段**:
  - `tables`: 必须完全匹配的表名列表
  - `ttl`: 缓存过期时间

#### 3.2.2 表格任意匹配 (tablesAny)
- **匹配逻辑**: 查询涉及的表格中任意一个匹配规则中的表格即可
- **示例**: 规则指定 `["users", "orders"]`，查询涉及 `users` 或 `orders` 任一表格都会匹配
- **配置字段**:
  - `tablesAny`: 包含任意一个即匹配的表名列表
  - `ttl`: 缓存过期时间

#### 3.2.3 表格全部匹配 (tablesAll)
- **匹配逻辑**: 查询涉及的表格必须包含规则中指定的所有表格
- **示例**: 规则指定 `["users", "orders"]`，查询必须同时涉及 `users` 和 `orders` 表格
- **配置字段**:
  - `tablesAll`: 必须全部包含的表名列表
  - `ttl`: 缓存过期时间

#### 3.2.4 查询ID匹配 (queryIds)
- **匹配逻辑**: 根据查询的唯一标识符进行匹配
- **示例**: 规则指定特定的查询ID列表，只有这些查询会被缓存
- **配置字段**:
  - `queryIds`: 查询ID列表
  - `ttl`: 缓存过期时间

#### 3.2.5 正则表达式匹配 (regex)
- **匹配逻辑**: 使用正则表达式匹配SQL语句
- **示例**: 规则指定正则表达式，匹配特定模式的SQL查询
- **配置字段**:
  - `regex`: SQL匹配的正则表达式
  - `ttl`: 缓存过期时间

#### 3.2.6 全局规则 (默认)
- **匹配逻辑**: 匹配所有查询，通常用于全局缓存策略
- **示例**: 对所有查询应用相同的缓存策略
- **配置字段**:
  - `ttl`: 默认缓存过期时间（通常为0，表示不缓存）

## 4. 查询信息数据结构

### 4.1 查询键存储

**存储位置**: `{datasourceName}:cache:query:{queryId}`

**存储格式**: JSON格式（序列化的查询对象）

**数据结构** (Redis String，存储序列化的查询对象):

**通用字段说明**:
- `id`: 查询唯一标识符
- `sql`: SQL查询语句
- `tables`: 涉及的数据表列表
- `count`: 查询执行次数
- `meanTime`: 平均执行时间（毫秒）
- `lastAccess`: 最后访问时间戳
- `cached`: 是否启用缓存

#### 4.1.1 用户资料查询
```json
{
  "id": "user_profile_by_id",
  "sql": "SELECT u.id, u.username, u.email, u.phone, p.first_name, p.last_name, p.avatar FROM users u LEFT JOIN user_profiles p ON u.id = p.user_id WHERE u.id = ?",
  "tables": ["users", "user_profiles"],
  "count": 2847,
  "meanTime": 12.3,
  "lastAccess": 1704067200000,
  "cached": true
}
```

#### 4.1.2 商品搜索查询
```json
{
  "id": "product_search",
  "sql": "SELECT p.id, p.name, p.price, p.description, p.image_url, c.name as category_name FROM products p JOIN categories c ON p.category_id = c.id WHERE p.status = 'active' AND c.name = ? ORDER BY p.created_at DESC LIMIT ?",
  "tables": ["products", "categories"],
  "count": 1523,
  "meanTime": 45.7,
  "lastAccess": 1704067140000,
  "cached": true
}
```

#### 4.1.3 订单历史查询
```json
{
  "id": "order_history",
  "sql": "SELECT o.id, o.order_number, o.total_amount, o.status, o.created_at, oi.product_name, oi.quantity, oi.price FROM orders o JOIN order_items oi ON o.id = oi.order_id WHERE o.user_id = ? ORDER BY o.created_at DESC LIMIT ?",
  "tables": ["orders", "order_items"],
  "count": 892,
  "meanTime": 67.2,
  "lastAccess": 1704067080000,
  "cached": true
}
```

#### 4.1.4 购物车查询
```json
{
  "id": "user_cart_items",
  "sql": "SELECT c.id, c.product_id, c.quantity, c.added_at, p.name, p.price, p.image_url, p.stock_quantity FROM cart_items c JOIN products p ON c.product_id = p.id WHERE c.user_id = ? AND p.status = 'active'",
  "tables": ["cart_items", "products"],
  "count": 1247,
  "meanTime": 23.8,
  "lastAccess": 1704067160000,
  "cached": true
}
```

#### 4.1.5 分类商品查询
```json
{
  "id": "category_products",
  "sql": "SELECT p.id, p.name, p.price, p.description, p.image_url, p.sales_count, p.rating FROM products p WHERE p.category_id = ? AND p.status = 'active' ORDER BY p.sales_count DESC, p.rating DESC LIMIT ?",
  "tables": ["products"],
  "count": 2156,
  "meanTime": 38.4,
  "lastAccess": 1704067100000,
  "cached": true
}
```

#### 4.1.6 库存检查查询
```json
{
  "id": "inventory_check",
  "sql": "SELECT i.product_id, i.available_quantity, i.reserved_quantity, i.last_updated, p.name FROM inventory i JOIN products p ON i.product_id = p.id WHERE i.product_id IN (?)",
  "tables": ["inventory", "products"],
  "count": 3421,
  "meanTime": 15.6,
  "lastAccess": 1704067220000,
  "cached": true
}
```

### 4.2 查询索引

**存储位置**: `{datasourceName}-cache-query-idx`

**用途**: RediSearch索引，用于快速查询和聚合统计

**索引字段**:
- `@queryId`: 查询ID
- `@sql`: SQL语句
- `@tables`: 涉及的表名
- `@cached`: 是否已缓存
- `@accessCount`: 访问次数
- `@meanTime`: 平均执行时间

### 4.3 查询管理索引

**按表名索引**: `{datasourceName}:cache:queries:by_table:{tableName}`
**按时间索引**: `{datasourceName}:cache:queries:by_time:{date}`
**按缓存状态索引**: `{datasourceName}:cache:queries:by_cache_status:{status}`

## 5. 缓存实现说明

### 5.1 缓存策略

当前架构采用基于规则的查询缓存策略，不直接存储查询结果数据。缓存的实现通过以下方式：

1. **规则匹配**: 根据配置的缓存规则匹配查询
2. **查询信息存储**: 存储查询的元数据信息
3. **统计信息收集**: 收集查询执行和缓存命中统计
4. **配置管理**: 动态管理缓存规则配置

### 5.2 缓存键命名规范

**查询信息键**: `{datasourceName}:cache:query:{queryId}`
**统计信息键**: `{datasourceName}:cache:stats:queries`
**配置确认键**: `{datasourceName}:config:ack`
**索引键**: `{datasourceName}-cache-query-idx`

## 5. 统计信息数据结构

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

### 5.2 统计信息实现说明

当前版本的统计信息主要通过以下方式实现：

1. **查询统计**: 通过RediSearch索引 `{datasourceName}-query-idx` 进行聚合查询
2. **缓存命中率**: 通过应用程序内存统计计算
3. **表格统计**: 基于查询信息动态计算
4. **慢查询**: 通过查询时间阈值过滤

### 5.3 RediSearch聚合查询示例

**查询统计聚合**:
```
FT.AGGREGATE {datasourceName}-query-idx 
  "*" 
  GROUPBY 1 @cached 
  REDUCE COUNT 0 AS count
```

**热门表格查询**:
```
FT.AGGREGATE {datasourceName}-query-idx 
  "*" 
  GROUPBY 1 @tables 
  REDUCE SUM 1 @accessCount AS totalAccess 
  SORTBY 2 @totalAccess DESC
```

## 6. 配置管理

### 6.1 配置确认机制

**存储位置**: `{datasourceName}:config:ack`

**数据类型**: Redis Sorted Set

**用途**: 跟踪各应用实例对配置更新的确认状态

**数据结构**:
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

## 7. Web API 数据格式

### 7.1 查询缓存状态 API

#### 7.1.1 用户资料查询缓存状态
**请求**: `GET /api/cache/query/user_profile_by_id`

**响应**:
```json
{
  "queryId": "user_profile_by_id",
  "cached": true,
  "cacheKey": "ecommerce_db:cache:user_profile_by_id",
  "ttl": 1650,
  "lastAccess": 1704067200000,
  "hitCount": 2847,
  "meanTime": 12.3,
  "cacheHitRate": 0.89
}
```

#### 7.1.2 商品搜索查询缓存状态
**请求**: `GET /api/cache/query/product_search`

**响应**:
```json
{
  "queryId": "product_search",
  "cached": true,
  "cacheKey": "ecommerce_db:cache:product_search:electronics_page1",
  "ttl": 420,
  "lastAccess": 1704067140000,
  "hitCount": 1523,
  "meanTime": 45.7,
  "cacheHitRate": 0.76
}
```

#### 7.1.3 购物车查询缓存状态
**请求**: `GET /api/cache/query/user_cart_items`

**响应**:
```json
{
  "queryId": "user_cart_items",
  "cached": true,
  "cacheKey": "ecommerce_db:cache:user_cart_items:user_12345",
  "ttl": 1650,
  "lastAccess": 1704067160000,
  "hitCount": 1247,
  "meanTime": 23.8,
  "cacheHitRate": 0.91
}
```

#### 7.1.4 库存检查查询缓存状态
**请求**: `GET /api/cache/query/inventory_check`

**响应**:
```json
{
  "queryId": "inventory_check",
  "cached": true,
  "cacheKey": "ecommerce_db:cache:inventory_check:batch_001",
  "ttl": 45,
  "lastAccess": 1704067220000,
  "hitCount": 3421,
  "meanTime": 15.6,
  "cacheHitRate": 0.95
}
```

### 7.2 查询管理接口

**获取查询列表响应**:
```json
{
  "queries": [
    {
      "id": "user_profile_by_id",
      "key": "ecommerce_db:cache:query:user_profile_by_id",
      "sql": "SELECT u.id, u.username, u.email, u.phone, p.first_name, p.last_name, p.avatar FROM users u LEFT JOIN user_profiles p ON u.id = p.user_id WHERE u.id = ?",
      "tables": ["users", "user_profiles"],
      "count": 2847,
      "meanTime": 12.3,
      "currentRule": {
        "id": "user_profile_cache",
        "ttl": 1800,
        "tables": ["users"]
      }
    },
    {
      "id": "product_search",
      "key": "ecommerce_db:cache:query:product_search",
      "sql": "SELECT p.id, p.name, p.price, p.description, p.image_url, c.name as category_name FROM products p JOIN categories c ON p.category_id = c.id WHERE p.status = 'active' AND c.name = ? ORDER BY p.created_at DESC LIMIT ?",
      "tables": ["products", "categories"],
      "count": 1523,
      "meanTime": 45.7,
      "currentRule": {
        "id": "product_search_cache",
        "ttl": 600,
        "tablesAny": ["products"]
      }
    }
  ],
  "total": 2
}
```

### 7.3 规则管理接口

**创建缓存规则请求**:
```json
{
  "ttl": 3600,
  "tables": ["users", "orders"],
  "priority": 1,
  "enabled": true
}
```

### 7.4 统计信息接口

**统计信息响应**:
```json
{
  "overallStats": {
    "totalQueries": 1000,
    "cachedQueries": 750,
    "cacheHitRate": 75.0,
    "avgResponseTime": 150.0
  },
  "topTables": [
    {
      "name": "users",
      "accessFrequency": 200,
      "queryTime": 120.0,
      "cacheHitRate": 80.0
    }
  ],
  "slowQueries": [
    {
      "queryId": "query_67890",
      "sql": "SELECT * FROM orders o JOIN users u ON o.user_id = u.id",
      "avgTime": 250.0,
      "executionCount": 50,
      "tables": ["orders", "users"]
    }
  ]
}
```