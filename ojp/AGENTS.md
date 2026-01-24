# Repository Guidelines

## Project Structure & Module Organization

OJP (Open JDBC Proxy) 是一个基于 JDBC 的数据库代理系统，提供缓存、监控、CDC 同步、SQL 翻译等功能。

### 核心模块

**后端 Java 模块** (基于 Spring Boot 3.3.3 + Maven):
- `ojp-server` - 核心代理服务，提供 gRPC 连接、缓存决策、监控等功能
- `ojp-cache` - 缓存决策模块，基于表同步状态进行查询路由决策
- `ojp-server-common` - 通用服务组件
- `ojp-grpc-commons` - gRPC 通信协议定义
- `ojp-jdbc-driver` - JDBC 驱动实现（支持 Java 11+）
- `ojp-jdbc-driver-java8` - Java 8 兼容的 JDBC 驱动
- `ojp-license` - 授权验证模块
- `ojp-cdc-monitor` - SeaTunnel CDC 状态监控插件

**Go 语言模块**:
- `ojp-sentinel` - eBPF 授权哨兵，利用内核级流量控制实现授权保护

**Python 服务**:
- `ojp-sql-translator` - SQL 翻译服务（基于 FastAPI + SQLGlot）

**前端应用**:
- `ojp-ui` - 基于 React 18 + Ant Design 5.x 的管理控制台

**示例服务**:
- `shopservice` - Spring Boot 示例应用，用于验证 OJP 集成

### 项目根目录

配置文件和脚本位于项目根目录：
- `pom.xml` - Maven 父 POM，管理所有 Java 模块依赖
- `build.bat` - Windows 构建脚本（检查环境、Maven 构建、Docker 镜像构建、服务启动）
- `README.md` - 项目主文档
- `AGENTS.md` - 本开发指南
- `logs/` - 运行日志目录

## Build, Test, and Development Commands

### 环境要求

- **Java**: 11+ (ojp-jdbc-driver-java8 支持 Java 8)
- **Maven**: 3.8+ (推荐使用 `mvnd` 加速构建)
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Node.js**: 16+ (前端开发)
- **Python**: 3.11+ (SQL 翻译服务)
- **Go**: 1.21+ (ojp-sentinel，可选)

### 构建命令

**完整构建** (Maven + Docker):
```bash
# Windows: 使用项目构建脚本
build.bat

# 手动执行步骤:
# 1. Maven 构建
mvn clean package install -DskipTests -Dmaven.javadoc.skip=true

# 2. 构建 shopservice
cd shopservice
mvn clean package -DskipTests
cd ..

# 3. Docker 镜像构建
docker-compose -f docker-compose.yml build ojp-server
docker-compose -f docker-compose.yml build ojp-ui
docker-compose -f docker-compose.yml build shopservice

# 4. 启动服务
docker-compose -f docker-compose.yml up -d
```

**模块构建**:
```bash
# 构建特定模块
mvn clean install -pl ojp-server -am -DskipTests

# 运行测试
mvn test
mvn test -pl ojp-cache
```

**前端开发**:
```bash
cd ojp-ui

# 安装依赖
npm install

# 开发模式（带代理）
npm run dev          # Vite 开发服务器（端口 5173）
npm run dev:full     # 同时启动 Node.js 代理和 Vite

# 生产构建
npm run build        # 构建到 dist/
npm run preview      # 预览生产构建

# 代码检查
npm run lint
```

**SQL 翻译服务**:
```bash
cd ojp-sql-translator

# 安装依赖
pip install -r requirements.txt

# 启动服务
python main.py
```

**ojp-sentinel 构建** (Go):
```bash
cd ojp-sentinel

# 构建 eBPF 字节码
go generate ./...

# 构建 Go 程序
go build -o sentinel ./cmd/sentinel
```

### 测试命令

**后端测试**:
```bash
# 运行所有测试
mvn test

# 运行集成测试（需要 Docker 环境）
mvn verify

# shopservice 集成测试（使用 H2 内存数据库）
cd shopservice
mvn clean verify
```

**前端测试**:
```bash
cd ojp-ui
npm run lint    # ESLint 检查
npm run build   # 构建验证
```

## Coding Style & Naming Conventions

### Java 模块

- **缩进**: 4 空格
- **包命名**: `org.openjdbcproxy.*`，按功能模块组织
- **配置**: YAML 格式，位于 `src/main/resources/`
- **注解**: 使用 Lombok 减少样板代码
- **编码**: UTF-8

### React 组件

- **缩进**: 2 空格
- **引号**: 单引号
- **组件**: 函数式组件，PascalCase 文件名
- **样式**: Ant Design 5.x 主题系统 + CSS3
- **工具函数**: `src/services` 或 `src/utils`

### Python 服务

- **遵循**: PEP 8 规范
- **类型**: 使用类型注解
- **异步**: FastAPI 异步路由

### Go 模块

- **格式**: `gofmt` 标准格式
- **命名**: Go 惯例（驼峰、导出大写）
- **注解**: 使用标准 Go 文档注释

## Architecture & Key Features

### 核心架构

```
┌─────────────────────────────────────────────────────────┐
│                    ojp-ui (React)                        │
│              管理控制台 / 监控面板                        │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/gRPC
┌────────────────────┴────────────────────────────────────┐
│                  ojp-server (Spring Boot)                │
│  ┌──────────────┬──────────────┬─────────────────────┐  │
│  │ gRPC Service │ Cache Engine │  Monitor/Actuator  │  │
│  └──────────────┴──────────────┴─────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼────────┐┌─▼──────────┐┌─▼──────────────┐
│   JDBC Driver  ││  Redis     ││  SQL Translator│
│  (客户端代理)   ││  (缓存)    ││   (Python)     │
└────────────────┘└────────────┘└────────────────┘
```

### 关键功能

**1. 智能缓存系统** (`ojp-cache`)
- 表格规则缓存（基于表名的全表缓存）
- 查询规则缓存（基于 SQL 模式的查询缓存）
- 正则规则缓存（基于正则表达式的灵活匹配）
- 慢查询识别和自动推荐
- 实时性能统计和命中率分析

**2. CDC 状态监控** (`ojp-cdc-monitor`)
- SeaTunnel 事件监听插件
- 实时推送 CDC 状态到 Redis
- 支持 Snapshot 和 Incremental 阶段监控

**3. SQL 翻译服务** (`ojp-sql-translator`)
- 基于 SQLGlot 的跨数据库 SQL 翻译
- 支持 MySQL ↔ PostgreSQL 等多种方言转换
- FastAPI REST 接口

**4. eBPF 授权哨兵** (`ojp-sentinel`)
- 内核级流量控制
- RSA 签名验证
- TC netem 延迟惩罚（未授权时 5 秒延迟）

**5. 管理控制台** (`ojp-ui`)
- 实时监控仪表盘
- 缓存规则管理
- SQL 统计和慢查询分析
- 系统测试（gRPC、数据库、缓存）
- Chinook SQL 实验台

## API Documentation

### RESTful API

**缓存模块** (`ojp-cache`):
- `GET /api/cache/rules/list` - 获取缓存规则列表
- `POST /api/cache/rules` - 创建/更新缓存规则
- `DELETE /api/cache/rules/{ruleId}` - 删除缓存规则
- `GET /api/cache/queries/list` - 获取慢查询列表（按连接分组）

**SQL 翻译服务**:
- `POST /translate` - SQL 翻译接口

详细 API 文档参见 `ojp-cache/RESTful_API文档.md` 和 `ojp-server/docs/RESTful API设计文档.md`。

### gRPC API

gRPC 协议定义位于 `ojp-grpc-commons/src/main/proto/`，支持：
- 连接管理
- 查询执行
- 缓存操作
- 监控指标上报

## Testing Guidelines

### 后端测试

- **框架**: JUnit 5 + Spring Boot Test
- **测试结构**: 镜像 `src/main` 包结构
- **命名**: 行为驱动命名（如 `shouldHandleBulkConnect`）
- **集成测试**: 使用 MockMvc + H2 内存数据库
- **Testcontainers**: 仅在 Docker Compose 暴露所需服务时使用

### 前端测试

- **Lint**: `npm run lint` (ESLint)
- **构建验证**: `npm run build`
- **手动测试**: Grafana 嵌入、Kong 路由、响应式布局

### 示例服务测试

`shopservice` 提供完整的集成测试套件：
```bash
cd shopservice
mvn clean verify  # 运行所有集成测试
```

## Commit & Pull Request Guidelines

### Commit 消息规范

- **风格**: 简短、祈使语气、中文
- **格式**: `添加grafana配置 支持iframe嵌入`
- **禁止**: 尾部标点符号

### Pull Request 要求

- **描述**: 列出范围、影响的 compose 文件/服务、配置变更
- **UI 变更**: 必须包含截图或 GIF
- **审查人**: 按影响区域请求审查
- **检查清单**: 跟踪后续事项（迁移、新路由等）

## Security & Configuration Tips

### 配置管理

- **文档**: 新服务文档放在 `docs/` 目录
- **密钥**: 使用未跟踪的 `.env` 文件，由 Docker Compose 消费
- **网络更新**: 网络映射变更时更新 `README.md` 的 WSL 路由指南
- **端点变更**: 端点调整后刷新 `PORTAL.md`

### 服务清理

删除服务后执行：
```bash
docker-compose down --remove-orphans
```

并在 `build.bat` 中反映 profile 变更。

### 安全最佳实践

- **授权验证**: 使用 RSA 签名（私钥不公开）
- **容器隔离**: ojp-sentinel 使用 `privileged: true` 和 `network_mode: host`
- **密钥管理**: 不在代码中硬编码凭据
- **依赖更新**: 定期更新依赖版本（Spring Boot、Jackson 等）

## Module-Specific Notes

### ojp-sql-translator

- **技术栈**: FastAPI + SQLGlot + Uvicorn
- **端口**: 8000 (默认)
- **依赖**: `requirements.txt`

### ojp-sentinel

- **技术栈**: Go 1.21 + eBPF + cilium/ebpf 库
- **权限**: 需要 `CAP_BPF` 和 `CAP_NET_ADMIN`
- **网络**: 必须使用 `network_mode: host`
- **文档**: `ojp-sentinel/ARCHITECTURE.md` 详细设计

### shopservice

- **数据库**: PostgreSQL (生产) / H2 (测试)
- **Chinook 集成**: 提供复杂 SQL 查询测试数据集
- **API**: RESTful CRUD + Chinook SQL 实验台

## Development Workflow

1. **拉取最新代码**: `git pull`
2. **构建项目**: `build.bat` 或 `mvn clean install`
3. **启动服务**: `docker-compose up -d`
4. **开发前端**: `cd ojp-ui && npm run dev`
5. **运行测试**: `mvn test` / `npm run lint`
6. **提交代码**: 遵循 commit 规范
7. **创建 PR**: 包含描述和截图（如适用）
