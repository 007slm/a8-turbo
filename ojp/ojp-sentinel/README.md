# OJP SQL Translator

OJP SQL Translator 是 Open JDBC Proxy 的 SQL 翻译服务模块，基于 Python FastAPI 和 SQLGlot 提供跨数据库 SQL 方言转换功能。

## 📋 目录

- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [配置说明](#配置说明)
- [部署方式](#部署方式)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- **跨数据库翻译**: 支持多种数据库方言间的 SQL 转换
- **Oracle → StarRocks**: 专门优化 Oracle 到 StarRocks 的转换
- **语法兼容性**: 处理不同数据库的语法差异
- **REST API**: 基于 FastAPI 的高性能 Web 服务
- **异步处理**: 支持异步 SQL 翻译请求

### 支持的数据库方言

- **源数据库**: Oracle, MySQL, PostgreSQL, SQL Server
- **目标数据库**: StarRocks, MySQL, PostgreSQL
- **主要优化**: Oracle → StarRocks 完整语法转换

### 技术特性

- **SQLGlot**: 强大的 SQL 解析和转换引擎
- **FastAPI**: 现代异步 Web 框架
- **Uvicorn**: 高性能 ASGI 服务器
- **Pydantic**: 数据验证和序列化
- **Docker**: 容器化部署支持

## 🏗️ 架构设计

### 组件架构

```
┌─────────────────────────────────────────────────────────┐
│                 OJP SQL Translator                       │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │   FastAPI    │   SQLGlot    │     Uvicorn        │  │
│  │   Routes     │   Engine     │     Server         │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/JSON
        ┌────────────┼────────────┐
        │            │            │
┌───────▼────────┐┌─▼──────────┐┌─▼──────────────┐
│   JDBC Driver  ││  ojp-ui     ││  External     │
│   (Auto Call)  ││  (Manual)   ││  Clients      │
└────────────────┘└─────────────┘└────────────────┘
```

### 核心模块

- **main.py**: FastAPI 应用主入口
- **translator.py**: SQL 翻译核心逻辑
- **models.py**: Pydantic 数据模型
- **dependencies.py**: 依赖注入和配置
- **utils.py**: 工具函数和辅助方法

## 🚀 快速开始

### 环境要求

- **Python**: 3.11+
- **pip**: 最新版本
- **Docker**: 20.10+ (可选，用于容器化部署)

### 安装依赖

```bash
# 克隆项目
cd ojp/ojp-sql-translator

# 安装依赖
pip install -r requirements.txt
```

### 运行服务

```bash
# 开发模式
python main.py

# 或使用 uvicorn
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

服务将在 http://localhost:8000 启动。

### Docker 部署

```bash
# 构建镜像
docker build -t ojp-sql-translator .

# 运行容器
docker run -p 8000:8000 ojp-sql-translator
```

## 📚 API 文档

### 基础信息

- **Base URL**: `http://localhost:8000`
- **API 文档**: `http://localhost:8000/docs` (Swagger UI)
- **OpenAPI 规范**: `http://localhost:8000/openapi.json`

### 主要接口

#### SQL 翻译接口

**端点**: `POST /translate`

**请求格式**:
```json
{
  "sql": "SELECT * FROM users WHERE id = ?",
  "source_dialect": "oracle",
  "target_dialect": "starrocks",
  "parameters": ["123"]
}
```

**响应格式**:
```json
{
  "translated_sql": "SELECT * FROM users WHERE id = 123",
  "source_dialect": "oracle",
  "target_dialect": "starrocks",
  "success": true,
  "execution_time": 0.001
}
```

**参数说明**:
| 参数 | 类型 | 必需 | 描述 |
|------|------|------|------|
| `sql` | string | 是 | 要翻译的 SQL 语句 |
| `source_dialect` | string | 是 | 源数据库方言 (oracle, mysql, postgresql, sqlserver) |
| `target_dialect` | string | 是 | 目标数据库方言 (starrocks, mysql, postgresql) |
| `parameters` | array | 否 | SQL 参数值，用于参数化查询 |

#### 批量翻译接口

**端点**: `POST /translate/batch`

**请求格式**:
```json
{
  "queries": [
    {
      "sql": "SELECT * FROM users",
      "source_dialect": "oracle",
      "target_dialect": "starrocks"
    },
    {
      "sql": "INSERT INTO orders VALUES (?, ?)",
      "source_dialect": "mysql",
      "target_dialect": "starrocks",
      "parameters": [1, "2024-01-01"]
    }
  ]
}
```

**响应格式**:
```json
{
  "results": [
    {
      "translated_sql": "SELECT * FROM users",
      "success": true,
      "execution_time": 0.001
    },
    {
      "translated_sql": "INSERT INTO orders VALUES (1, '2024-01-01')",
      "success": true,
      "execution_time": 0.002
    }
  ],
  "total_time": 0.003
}
```

#### 健康检查接口

**端点**: `GET /health`

**响应格式**:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-01T12:00:00Z",
  "version": "1.0.0"
}
```

### 错误处理

**错误响应格式**:
```json
{
  "error": "SQL_SYNTAX_ERROR",
  "message": "Invalid SQL syntax",
  "details": "Expected SELECT, found INSERT at position 0",
  "execution_time": 0.001
}
```

**常见错误码**:
- `SQL_SYNTAX_ERROR`: SQL 语法错误
- `UNSUPPORTED_DIALECT`: 不支持的数据库方言
- `TRANSLATION_FAILED`: 翻译失败
- `PARAMETER_MISMATCH`: 参数不匹配

## ⚙️ 配置说明

### 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `HOST` | 服务监听地址 | `0.0.0.0` |
| `PORT` | 服务端口 | `8000` |
| `WORKERS` | Uvicorn 工作进程数 | `1` |
| `LOG_LEVEL` | 日志级别 | `INFO` |
| `MAX_SQL_LENGTH` | 最大 SQL 长度限制 | `10000` |

### 应用配置

```python
# config.py
class Settings:
    host: str = "0.0.0.0"
    port: int = 8000
    max_sql_length: int = 10000
    supported_source_dialects = ["oracle", "mysql", "postgresql", "sqlserver"]
    supported_target_dialects = ["starrocks", "mysql", "postgresql"]
```

## 🚢 部署方式

### Docker Compose 集成

```yaml
version: '3.8'
services:
  sql-translator:
    build: ./ojp-sql-translator
    ports:
      - "8000:8000"
    environment:
      - HOST=0.0.0.0
      - PORT=8000
    networks:
      - ojp-network
```

### Kubernetes 部署

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sql-translator
spec:
  replicas: 2
  selector:
    matchLabels:
      app: sql-translator
  template:
    metadata:
      labels:
        app: sql-translator
    spec:
      containers:
      - name: sql-translator
        image: ojp-sql-translator:latest
        ports:
        - containerPort: 8000
        env:
        - name: HOST
          value: "0.0.0.0"
        - name: PORT
          value: "8000"
```

## 🔧 故障排查

### 常见问题

#### 1. 翻译结果不正确

**可能原因**:
- SQL 语法复杂，SQLGlot 解析有限制
- 特定数据库的扩展语法不支持
- 参数绑定处理不当

**解决方法**:
- 检查 SQLGlot 支持的语法范围
- 简化复杂查询
- 验证参数传递格式

#### 2. 服务响应慢

**可能原因**:
- SQL 语句过长
- 复杂查询解析耗时
- 并发请求过多

**解决方法**:
- 限制最大 SQL 长度
- 优化 SQLGlot 配置
- 增加工作进程数

#### 3. 内存使用过高

**可能原因**:
- 大量并发翻译请求
- SQL 语句包含大量数据

**解决方法**:
- 实现请求队列限制
- 添加超时机制
- 监控内存使用情况

### 日志分析

```bash
# 查看应用日志
docker logs sql-translator

# 查看 uvicorn 访问日志
tail -f /var/log/uvicorn.log
```

### 性能监控

```bash
# 健康检查
curl http://localhost:8000/health

# 性能指标 (需要配置 prometheus)
curl http://localhost:8000/metrics
```

## 🤝 开发指南

### 项目结构

```
ojp-sql-translator/
├── main.py              # FastAPI 应用入口
├── requirements.txt     # Python 依赖
├── Dockerfile          # Docker 构建文件
├── models.py           # Pydantic 数据模型
├── translator.py       # SQL 翻译核心逻辑
├── utils.py            # 工具函数
├── tests/              # 测试文件
│   ├── test_translator.py
│   └── test_api.py
└── docs/               # 文档 (如果需要)
```

### 添加新的数据库支持

1. **扩展方言支持**:
```python
# translator.py
def translate_sql(sql: str, source: str, target: str) -> str:
    # 添加新的方言映射
    dialect_map = {
        "newdb": "newdb_dialect"
    }
    # 使用 SQLGlot 进行转换
```

2. **添加测试用例**:
```python
# tests/test_translator.py
def test_new_dialect_translation():
    translator = SQLTranslator()
    result = translator.translate("SELECT * FROM table", "newdb", "starrocks")
    assert result.success == True
```

### 扩展 API

1. **添加新端点**:
```python
# main.py
@app.post("/validate")
async def validate_sql(request: ValidateRequest):
    # SQL 验证逻辑
    pass
```

2. **更新数据模型**:
```python
# models.py
class ValidateRequest(BaseModel):
    sql: str
    dialect: str
```

## 📊 监控指标

### 内置指标

- **翻译请求数**: 按源/目标方言统计
- **翻译成功率**: 成功翻译比例
- **平均翻译时间**: 按方言分组的性能指标
- **错误统计**: 按错误类型统计

### 集成 Prometheus

```python
from prometheus_client import Counter, Histogram

translation_requests = Counter('sql_translations_total', 'Total SQL translations', ['source', 'target'])
translation_duration = Histogram('sql_translation_duration_seconds', 'SQL translation duration', ['source', 'target'])
```

## 📄 许可证

本项目采用 MIT 许可证。</content>
<parameter name="filePath">E:\a8-turbo\ojp\ojp-sql-translator\README.md