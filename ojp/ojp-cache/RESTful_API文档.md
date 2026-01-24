# Open JDBC Proxy 缓存模块 RESTful API 文档

## 1. 概述

本文档描述了 Open JDBC Proxy 缓存模块提供的 RESTful API 接口。这些接口用于管理缓存规则、查询统计和表格性能数据，支持前端调用展示相关数据。

## 2. 接口详情

### 2.1 缓存规则管理接口

#### 获取缓存规则列表

**接口地址**: `GET /api/cache/rules/list`

**接口描述**: 获取系统中所有的缓存规则，按数据库分组返回

**请求参数**: 无

**返回结果**:
```json
{
  "user_service_db": [
    {
      "ruleId": "string",
      "ruleName": "string",
      "datasourceName": "string",
      "dbType": "string",
      "tables": ["string"],
      "ruleType": "TABLES",
      "ttl": 0,
      "enabled": true,
      "description": "string",
      "priority": 0,
      "createdAt": "2024-01-01T12:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "matchedQueries": ["string"]
    }
  ]
}
```

**字段说明**:
| 字段名 | 类型 | 描述 |
|--------|------|------|
| ruleId | string | 规则唯一ID |
| ruleName | string | 规则名称 |
| datasourceName | string | 数据库名称 |
| dbType | string | 数据库类型 (mysql, postgresql, oracle) |
| tables | array | 涉及的表名列表 |
| ruleType | string | 规则类型 (TABLES, TABLES_ANY, TABLES_ALL, QUERY_IDS, REGEX) |
| ttl | integer | 缓存过期时间(秒) |
| enabled | boolean | 是否启用 |
| description | string | 规则描述 |
| priority | integer | 优先级 |
| createdAt | string | 创建时间 (ISO格式) |
| updatedAt | string | 更新时间 (ISO格式) |
| matchedQueries | array | 匹配的查询ID列表 |

#### 创建缓存规则

**接口地址**: `POST /api/cache/rules`

**接口描述**: 创建新的缓存规则

**请求参数**:
```json
{
  "ruleName": "string",
  "dbType": "string",
  "ttl": 0,
  "ruleType": "TABLES",
  "tables": ["string"],
  "priority": 0,
  "enabled": true,
  "description": "string"
}
```

**参数说明**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| ruleName | string | 是 | 规则名称 |
| dbType | string | 否 | 数据库类型 |
| ttl | integer | 否 | 缓存时间(秒) |
| ruleType | string | 否 | 规则类型 (TABLES, TABLES_ANY, TABLES_ALL, QUERY_IDS, REGEX) |
| tables | array | 否 | 精确匹配的表名列表 |
| tablesAny | array | 否 | 任意匹配的表名列表 |
| tablesAll | array | 否 | 全部匹配的表名列表 |
| queryIds | array | 否 | 查询ID列表 |
| regex | string | 否 | 正则表达式 |
| priority | integer | 否 | 优先级 |
| enabled | boolean | 是 | 是否启用 |
| description | string | 否 | 规则描述 |

**返回结果**:
```json
{
  "id": "string",
  "ruleName": "string",
  "dbType": "string",
  "ttl": 0,
  "ruleType": "TABLES",
  "tables": ["string"],
  "priority": 0,
  "enabled": true,
  "description": "string",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

#### 更新缓存规则

**接口地址**: `PUT /api/cache/rules/{ruleId}`

**接口描述**: 更新指定的缓存规则

**请求参数**:
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|
| ruleId | string | path | 是 | 规则ID |

**请求体**: 同创建规则

**返回结果**: 同创建规则响应

#### 删除缓存规则

**接口地址**: `DELETE /api/cache/rules/{ruleId}`

**接口描述**: 删除指定的缓存规则

**请求参数**:
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|
| ruleId | string | path | 是 | 要删除的规则ID |

**返回结果**: 200 OK 表示删除成功

### 2.2 查询管理接口

#### 获取查询列表

**接口地址**: `GET /api/cache/queries/list`

**接口描述**: 获取所有查询统计信息，按数据库分组返回

**请求参数**: 无

**返回结果**:
```json
{
  "user_service_db": [
    {
      "queryId": "string",
      "sql": "string",
      "tables": ["string"],
      "accessCount": 0,
      "meanQueryTime": 0.0,
      "lastAccess": "2024-01-01T12:00:00Z",
      "cached": true,
      "cacheHitRate": 0.0
    }
  ]
}
```

**字段说明**:
| 字段名 | 类型 | 描述 |
|--------|------|------|
| queryId | string | 查询ID |
| sql | string | SQL语句 |
| tables | array | 涉及的表名集合 |
| accessCount | integer | 访问次数 |
| meanQueryTime | number | 平均查询时间(毫秒) |
| lastAccess | string | 最后访问时间 (ISO格式) |
| cached | boolean | 是否已缓存 |
| cacheHitRate | number | 缓存命中率 |

#### 获取查询详情

**接口地址**: `GET /api/cache/queries/{queryId}/details`

**接口描述**: 获取指定查询的详细信息

**请求参数**:
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|
| queryId | string | path | 是 | 查询ID |

**返回结果**:
```json
{
  "queryId": "string",
  "datasourceName": "string",
  "sql": "string",
  "tables": ["string"],
  "accessCount": 0,
  "meanQueryTime": 0.0,
  "maxQueryTime": 0.0,
  "minQueryTime": 0.0,
  "lastAccess": "2024-01-01T12:00:00Z",
  "cached": true,
  "cacheHitRate": 0.0,
  "currentRule": {
    "id": "string",
    "ttl": 0,
    "ruleType": "TABLES",
    "tables": ["string"],
    "priority": 0,
    "enabled": true,
    "description": "string"
  },
  "cacheStatus": {
    "cacheKey": "string",
    "ttlRemaining": 0,
    "cacheSize": 0,
    "lastCacheUpdate": "2024-01-01T12:00:00Z"
  }
}
```

### 2.3 表格统计接口

#### 获取表格统计列表

**接口地址**: `GET /api/cache/stats/tables/list`

**接口描述**: 获取所有表格的统计信息，按数据库分组返回

**请求参数**: 无

**返回结果**:
```json
{
  "user_service_db": [
    {
      "name": "string",
      "accessFrequency": 0,
      "avgQueryTime": 0.0,
      "cacheHitRate": 0.0,
      "currentRule": {
        "id": "string",
        "ttl": 0,
        "ruleType": "TABLES",
        "enabled": true
      },
      "relatedQueries": ["string"]
    }
  ]
}
```

#### 获取表格详细统计

**接口地址**: `GET /api/cache/stats/tables/{tableName}/details`

**接口描述**: 获取指定表格的详细统计信息

**请求参数**:
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|
| tableName | string | path | 是 | 表名 |

**返回结果**:
```json
{
  "name": "string",
  "accessFrequency": 0,
  "avgQueryTime": 0.0,
  "maxQueryTime": 0.0,
  "minQueryTime": 0.0,
  "cacheHitRate": 0.0,
  "totalCacheSize": 0,
  "currentRule": {
    "id": "string",
    "ttl": 0,
    "ruleType": "TABLES",
    "tables": ["string"],
    "priority": 0,
    "enabled": true,
    "description": "string"
  },
  "relatedQueries": [
    {
      "queryId": "string",
      "accessCount": 0,
      "avgTime": 0.0,
      "cached": true
    }
  ],
  "performanceHistory": [
    {
      "timestamp": "2024-01-01T11:00:00Z",
      "avgQueryTime": 0.0,
      "accessCount": 0,
      "cacheHitRate": 0.0
    }
  ]
}
```

## 3. 错误处理

所有接口在出现错误时都会返回标准的 HTTP 错误码和相应的错误信息:

- 200 OK: 请求成功
- 201 Created: 资源创建成功
- 400 Bad Request: 请求参数错误
- 404 Not Found: 资源不存在
- 409 Conflict: 资源冲突
- 500 Internal Server Error: 服务器内部错误

错误响应格式:
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

## 4. 注意事项

1. 所有时间相关的字段均采用 ISO 8601 格式 (yyyy-MM-ddTHH:mm:ssZ)
2. 所有接口均返回 JSON 格式数据
3. 接口路径均以 `/api/cache` 开头
4. 数据按数据库名称自然分组，简化数据结构
5. 缓存规则支持多种匹配类型：TABLES(精确匹配)、TABLES_ANY(任意匹配)、TABLES_ALL(全部匹配)、QUERY_IDS(查询ID匹配)、REGEX(正则表达式匹配)