# Open JDBC Proxy 缓存模块 RESTful API 文档

## 1. 概述

本文档描述了 Open JDBC Proxy 缓存模块提供的 RESTful API 接口。这些接口主要用于管理缓存规则和查看慢查询信息，供前端调用展示相关数据。

## 2. 接口详情

### 2.1 缓存规则管理接口

#### 获取缓存规则列表

**接口地址**: `GET /api/cache/rules/list`

**接口描述**: 获取系统中所有的缓存规则

**请求参数**: 无

**返回结果**: 
```json
[
  {
    "id": "string",
    "name": "string",
    "description": "string",
    "slowQueryIds": ["string"],
    "enabled": true,
    "connHash": "string",
    "createdAt": "yyyy-MM-dd HH:mm:ss",
    "updatedAt": "yyyy-MM-dd HH:mm:ss"
  }
]
```

**示例**:
```json
[
  {
    "id": "rule1",
    "name": "用户查询缓存规则",
    "description": "缓存用户相关的查询",
    "slowQueryIds": ["query1", "query2"],
    "enabled": true,
    "connHash": "db1_hash",
    "createdAt": "2025-01-01 12:00:00",
    "updatedAt": "2025-01-01 12:00:00"
  }
]
```

#### 创建或更新缓存规则

**接口地址**: `POST /api/cache/rules`

**接口描述**: 创建新的缓存规则或更新现有规则

**请求参数**:
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "slowQueryIds": ["string"],
  "enabled": true,
  "connHash": "string"
}
```

**参数说明**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| id | string | 否 | 规则ID，为空时表示创建新规则 |
| name | string | 是 | 规则名称 |
| description | string | 否 | 规则描述 |
| slowQueryIds | array | 否 | 关联的慢查询ID列表 |
| enabled | boolean | 是 | 是否启用规则 |
| connHash | string | 是 | 数据库连接哈希值 |

**返回结果**:
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "slowQueryIds": ["string"],
  "enabled": true,
  "connHash": "string",
  "createdAt": "yyyy-MM-dd HH:mm:ss",
  "updatedAt": "yyyy-MM-dd HH:mm:ss"
}
```

#### 删除缓存规则

**接口地址**: `DELETE /api/cache/rules/{ruleId}`

**接口描述**: 根据规则ID删除指定的缓存规则

**请求参数**:
| 参数名 | 类型 | 位置 | 必需 | 描述 |
|--------|------|------|------|------|
| ruleId | string | path | 是 | 要删除的规则ID |

**返回结果**: 200 OK 表示删除成功

### 2.2 查询管理接口

#### 获取查询列表

**接口地址**: `GET /api/cache/queries/list`

**接口描述**: 获取所有慢查询信息，并按照数据库连接哈希值进行分组

**请求参数**: 无

**返回结果**:
```json
{
  "connHash1": [
    {
      "id": "string",
      "sql": "string",
      "parameters": "string",
      "executionTime": 0,
      "inTransaction": true,
      "hasError": true,
      "timestamp": 0,
      "clientUUID": "string",
      "connHash": "string",
      "methodName": "string",
      "normalizedSql": "string",
      "queryType": "string",
      "tableNames": "string"
    }
  ]
}
```

**字段说明**:
| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | string | 查询ID |
| sql | string | SQL语句 |
| parameters | string | SQL参数 |
| executionTime | long | 执行时间(毫秒) |
| inTransaction | boolean | 是否在事务中执行 |
| hasError | boolean | 是否执行出错 |
| timestamp | long | 时间戳 |
| clientUUID | string | 客户端UUID |
| connHash | string | 数据库连接哈希值 |
| methodName | string | 方法名称 |
| normalizedSql | string | 标准化SQL |
| queryType | string | 查询类型(SELECT/INSERT/UPDATE/DELETE等) |
| tableNames | string | 涉及的表名列表 |

**示例**:
```json
{
  "db1_conn_hash": [
    {
      "id": "query1",
      "sql": "SELECT * FROM users WHERE id = ?",
      "parameters": "[1]",
      "executionTime": 1500,
      "inTransaction": false,
      "hasError": false,
      "timestamp": 1700000000000,
      "clientUUID": "client1",
      "connHash": "db1_conn_hash",
      "methodName": "executeQuery",
      "normalizedSql": "SELECT * FROM users WHERE id = ?",
      "queryType": "SELECT",
      "tableNames": "users"
    }
  ]
}
```

## 3. 错误处理

所有接口在出现错误时都会返回标准的 HTTP 错误码和相应的错误信息:

- 400 Bad Request: 请求参数错误
- 404 Not Found: 请求的资源不存在
- 500 Internal Server Error: 服务器内部错误

错误响应格式:
```json
{
  "timestamp": "2025-01-01T12:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "错误详细信息",
  "path": "/api/cache/rules/nonexistent"
}
```

## 4. 注意事项

1. 所有时间相关的字段均采用 `yyyy-MM-dd HH:mm:ss` 格式
2. 所有接口均返回 JSON 格式数据
3. 接口路径均以 `/api/cache` 开头
4. 数据存储在 Redis 中，实体类使用了 Spring Data Redis 注解