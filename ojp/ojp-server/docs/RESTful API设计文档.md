# RESTful API 设计文档

## 1. API 概述

基于CLI程序的功能分析，RESTful API提供以下核心功能：
- **查询管理**: 获取查询列表、查询详情、查询统计
- **规则管理**: 创建、更新、删除缓存规则
- **表格统计**: 获取表格访问统计和性能数据
- **缓存管理**: 查看缓存状态

## 2. API 基础信息

**Base URL**: `/api/cache`

**响应格式**: JSON

**HTTP状态码**:
- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未授权访问
- `404 Not Found`: 资源不存在
- `409 Conflict`: 资源冲突
- `500 Internal Server Error`: 服务器内部错误

## 3. 查询管理 API

### 3.1 获取查询列表

**请求**:
```http
GET /api/cache/queries/list
```

**实现细节**:
- 扫描Redis中所有查询统计数据的键（使用配置的键前缀模式）
- 解析每个查询的基础统计信息（访问次数、响应时间等）
- 检查查询是否有缓存（简单的存在性检查）
- 按数据库名称对查询进行分组（基于Redis键中的datasourceName自然分组，无需额外分组逻辑）
- 读取缓存命中率：从Redis中读取已存储的命中率统计数据（运行时由系统自动更新）

**响应**:
```json
{
  "user_service_db": [
    {
      "queryId": "user_profile_by_id",
      "sql": "SELECT u.id, u.username, u.email FROM users u WHERE u.id = ?",
      "tables": ["users", "user_profiles"],
      "accessCount": 2847,
      "meanQueryTime": 12.3,
      "lastAccess": "2024-01-01T12:00:00Z",
      "cached": true,
      "cacheHitRate": 0.89
    }
  ],
  "order_service_db": [
    {
      "queryId": "order_details_by_id",
      "sql": "SELECT * FROM orders WHERE id = ?",
      "tables": ["orders"],
      "accessCount": 1523,
      "meanQueryTime": 8.7,
      "lastAccess": "2024-01-01T11:45:00Z",
      "cached": false,
      "cacheHitRate": 0.0
    }
  ]
}
```

### 3.2 获取查询详情（含关联信息）

**请求**:
```http
GET /api/cache/queries/{queryId}/details
```

**路径参数**:
- `queryId` (required): 查询ID

**实现细节**:
- 根据queryId构造查询统计数据的Redis键，获取查询的详细统计信息
- 解析查询统计数据（访问次数、平均响应时间、最大最小响应时间等）
- 遍历缓存规则配置，根据查询涉及的表名匹配当前生效的缓存规则
- 获取查询对应缓存的实时状态信息（TTL剩余时间、缓存大小、最后更新时间等）
- 验证查询是否存在，不存在则返回404状态码

**响应**:
```json
{
  "queryId": "user_profile_by_id",
  "datasourceName": "user_service_db",
  "sql": "SELECT u.id, u.username, u.email, u.phone, p.first_name, p.last_name, p.avatar FROM users u LEFT JOIN user_profiles p ON u.id = p.user_id WHERE u.id = ?",
  "tables": ["users", "user_profiles"],
  "accessCount": 2847,
  "meanQueryTime": 12.3,
  "maxQueryTime": 45.2,
  "minQueryTime": 3.1,
  "lastAccess": "2024-01-01T12:00:00Z",
  "cached": true,
  "cacheHitRate": 0.89,
  "currentRule": {
      "id": "user_profile_cache",
      "ttl": 1800,
      "ruleType": "TABLES",
      "tables": ["users"],
      "priority": 1,
      "enabled": true,
      "description": "用户基础信息缓存30分钟"
    },
  "cacheStatus": {
    "cacheKey": "user_service_db:cache:user_profile_by_id",
    "ttlRemaining": 1650,
    "cacheSize": 1024,
    "lastCacheUpdate": "2024-01-01T11:45:00Z"
  }
}
```



## 4. 规则管理 API

### 4.1 获取规则列表

**请求**:
```http
GET /api/cache/rules/list
```

**实现细节**:
- 从Redis中获取缓存规则配置数据（使用配置的规则存储键）
- 解析规则配置的JSON格式数据，获取所有规则定义
- 遍历所有查询统计数据，统计每个规则匹配的查询数量（基于表名匹配逻辑）
- 根据Redis键中的datasourceName按数据库进行自然分组
- 按规则优先级对每个应用内的规则列表进行排序

**响应**:
```json
{
  "user_service_db": [
    {
      "id": "user_profile_cache",
      "ttl": 1800,
      "ruleType": "TABLES",
      "tables": ["users"],
      "priority": 1,
      "enabled": true,
      "description": "用户基础信息缓存30分钟",
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:00:00Z",
      "matchedQueries": 15
    }
  ],
  "product_service_db": [
    {
      "id": "product_search_cache",
      "ttl": 600,
      "ruleType": "TABLES_ANY",
      "tablesAny": ["products"],
      "priority": 2,
      "enabled": true,
      "description": "商品搜索结果缓存10分钟",
      "createdAt": "2024-01-01T10:05:00Z",
      "updatedAt": "2024-01-01T10:05:00Z",
      "matchedQueries": 8
    }
  ]
}
```

### 4.2 创建缓存规则

**请求**:
```http
POST /api/cache/rules
Content-Type: application/json

{
  "ruleName": "用户订单关联查询缓存规则",
  "dbType": "mysql",
  "ttl": 3600,
  "ruleType": "TABLES",
  "tables": ["users", "orders"],
  "priority": 1,
  "enabled": true,
  "description": "用户订单关联查询缓存1小时"
}
```

**实现细节**:
- 验证请求体中的规则配置合法性（TTL格式、表名有效性、规则类型等）
- 检查规则ID的唯一性，如果已存在则返回409冲突状态
- 将新规则添加到Redis中的规则配置存储结构中
- 设置规则的创建时间和更新时间戳
- 为新规则设置初始的统计数据（如匹配查询数量为0）
- 如果规则处于启用状态，立即将其应用到符合条件的查询上

**请求体字段说明**:
- `ruleName` (optional): 规则名称
- `dbType` (optional): 数据库类型 (mysql, postgresql, oracle)
- `ttl` (optional): 缓存时间，秒为单位的整数
- `ruleType` (optional): 规则类型 (`TABLES`, `TABLES_ANY`, `TABLES_ALL`, `QUERY_IDS`, `REGEX`)
- `tables` (optional): 精确匹配的表名列表
- `tablesAny` (optional): 任意匹配的表名列表
- `tablesAll` (optional): 全部匹配的表名列表
- `queryIds` (optional): 查询ID列表
- `regex` (optional): 正则表达式
- `priority` (optional): 优先级
- `enabled` (optional): 是否启用
- `description` (optional): 规则描述

**响应**:
```json
{
  "id": "rule_1704067200",
  "ruleName": "用户订单关联查询缓存规则",
  "dbType": "mysql",
  "ttl": 3600,
  "ruleType": "TABLES",
  "tables": ["users", "orders"],
  "priority": 1,
  "enabled": true,
  "description": "用户订单关联查询缓存1小时",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

### 4.3 更新缓存规则

**请求**:
```http
PUT /api/cache/rules/{ruleId}
Content-Type: application/json

{
  "ruleName": "用户订单关联查询缓存规则（更新）",
  "ttl": 7200,
  "enabled": false,
  "description": "临时禁用的用户订单关联查询缓存"
}
```

**路径参数**:
- `ruleId` (required): 规则ID

**实现细节**:
- 从Redis中获取指定规则ID的现有配置
- 验证更新后的规则配置的有效性和完整性
- 在Redis中更新对应规则的配置数据
- 更新规则的最后修改时间戳
- 如果规则的启用状态或关键配置发生变化，重新评估并应用到相关查询
- 如果规则被禁用或删除，清除使用该规则的查询缓存数据

**响应**:
```json
{
  "id": "rule_1704067200",
  "ruleName": "用户订单关联查询缓存规则（更新）",
  "dbType": "mysql",
  "ttl": 7200,
  "ruleType": "TABLES",
  "tables": ["users", "orders"],
  "priority": 1,
  "enabled": false,
  "description": "临时禁用的用户订单关联查询缓存",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:30:00Z"
}
```

### 4.4 删除缓存规则

**请求**:
```http
DELETE /api/cache/rules/{ruleId}
```

**路径参数**:
- `ruleId` (required): 规则ID

**实现细节**:
- 验证指定规则ID是否存在，不存在则返回404状态码
- 从Redis的规则配置存储中移除指定的规则
- 识别并清除所有使用该规则的查询缓存数据
- 更新相关查询的缓存规则匹配状态和统计信息
- 记录规则删除操作的审计日志

**响应**:
```json
{
  "message": "缓存规则已成功删除"
}
```

## 5. 表格统计 API

### 5.1 获取表格统计列表

**请求**:
```http
GET /api/cache/stats/tables/list
```

**实现细节**:
- 遍历所有查询统计数据，按查询涉及的数据表进行分组聚合
- 计算每个表的总访问频率（汇总所有相关查询的访问次数）
- 计算表级别的平均查询时间（基于相关查询的加权平均值）
- 读取表级别的缓存命中率（从Redis中读取已存储的统计数据，运行时由系统自动更新）
- 匹配每个表当前生效的缓存规则配置
- 根据Redis键中的datasourceName按数据库进行自然分组返回

**响应**:
```json
{
  "user_service_db": [
    {
      "name": "users",
      "accessFrequency": 2847,
      "avgQueryTime": 12.3,
      "cacheHitRate": 0.89,
      "currentRule": {
        "id": "user_profile_cache",
        "ttl": 1800,
        "ruleType": "TABLES",
        "enabled": true
      },
      "relatedQueries": [
        "user_profile_by_id",
        "user_authentication",
        "user_preferences"
      ]
    }
  ],
  "product_service_db": [
    {
      "name": "products",
      "accessFrequency": 1523,
      "avgQueryTime": 45.7,
      "cacheHitRate": 0.76,
      "currentRule": {
        "id": "product_search_cache",
        "ttl": 600,
        "ruleType": "TABLES_ANY",
        "enabled": true
      },
      "relatedQueries": [
        "product_search",
        "category_products",
        "product_details"
      ]
    }
  ]
}
```

### 5.2 获取表格详细统计（含关联信息）

**请求**:
```http
GET /api/cache/stats/tables/{tableName}/details
```

**路径参数**:
- `tableName` (required): 表名

**实现细节**:
- 遍历查询统计数据，筛选出所有涉及指定表名的查询
- 聚合这些查询的统计数据（总访问次数、平均响应时间、缓存命中情况等）
- 计算该表的整体缓存性能指标和趋势分析
- 查找当前应用于该表的所有缓存规则配置
- 收集并返回所有相关查询的标识列表
- 如果指定表名没有相关查询记录，返回404状态码

**响应**:
```json
{
  "name": "users",
  "accessFrequency": 2847,
  "avgQueryTime": 12.3,
  "maxQueryTime": 45.2,
  "minQueryTime": 3.1,
  "cacheHitRate": 0.89,
  "totalCacheSize": 15360,
  "currentRule": {
    "id": "user_profile_cache",
    "ttl": 1800,
    "ruleType": "TABLES",
    "tables": ["users"],
    "priority": 1,
    "enabled": true,
    "description": "用户基础信息缓存30分钟"
  },
  "relatedQueries": [
    {
      "queryId": "user_profile_by_id",
      "accessCount": 2000,
      "avgTime": 10.5,
      "cached": true
    },
    {
      "queryId": "user_authentication",
      "accessCount": 500,
      "avgTime": 15.2,
      "cached": true
    },
    {
      "queryId": "user_preferences",
      "accessCount": 347,
      "avgTime": 18.7,
      "cached": true
    }
  ],
  "performanceHistory": [
    {
      "timestamp": "2024-01-01T11:00:00Z",
      "avgQueryTime": 11.8,
      "accessCount": 150,
      "cacheHitRate": 0.87
    },
    {
      "timestamp": "2024-01-01T12:00:00Z",
      "avgQueryTime": 12.3,
      "accessCount": 180,
      "cacheHitRate": 0.89
    }
  ]
}
```

## 6. 错误响应格式

**通用错误响应**:
```json
{
  "code": "RULE_NOT_FOUND",
  "message": "指定的缓存规则不存在",
  "details": {
    "ruleId": "non_existent_rule",
    "datasourceName": "ecommerce_db"
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**验证错误响应**:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数验证失败",
  "details": {
    "fieldErrors": [
      {
        "field": "ttl",
        "message": "TTL格式不正确，请使用秒为单位的整数",
        "rejectedValue": "invalid_ttl"
      },
      {
        "field": "ruleType",
        "message": "规则类型必须是TABLES、TABLES_ANY、TABLES_ALL、QUERY_IDS或REGEX之一",
        "rejectedValue": "INVALID_TYPE"
      }
    ]
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## 7. API 配置

### 7.1 配置示例

```yaml
fluxCache:
  redis:
    host: "localhost"
    port: 6379
    database: 0
    password: ""
    username: ""
    timeout: 5000
    ssl: false
```

### 7.2 数据库配置

```yaml
fluxCache:
  database:
    name: "ecommerce_db"           # 默认数据库名称
```