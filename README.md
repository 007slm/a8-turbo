# A8 Turbo - Open JDBC Proxy (OJP) 集成环境

**A8 Turbo** 是 **Open-JDBC-Proxy (OJP)** 项目（服务名称：**a8-db**）的集成开发与部署环境。它提供了一个完整的生态系统，包括数据库中间件、缓存层、SQL 翻译 Sidecar 以及数据同步工具。

## 1. 核心架构

*   **A8 DB Proxy (`a8-dbproxy`)**: 原称 `ojp-server`。核心 gRPC 中间件，负责拦截 JDBC 请求，基于表同步状态进行智能路由，并管理数据同步。
*   **A8 UI (`a8-ui`)**: 原称 `ojp-ui`。基于 Web 的管理门户 (React/Vite)。
*   **A8 SQL Translator (`ojp-sql-translator`)**: 一个基于 Python 的 Sidecar 服务 (使用 SQLGlot)，用于将 Oracle SQL 转换为 StarRocks SQL 以实现透明缓存。
*   **组件 (Addons)**: SeaTunnel (CDC/同步), Redis (缓存), MySQL (源库), StarRocks (OLAP/缓存存储)。

## 2. 项目结构

### 源代码 (`ojp/`)
*   `ojp-server` (a8-dbproxy): 核心 Java 服务。
*   `ojp-cache`: 缓存逻辑与规则管理模块 (a8-dbproxy 的依赖库)。
*   `ojp-sql-translator`: 用于 SQL 翻译的 Python Sidecar。
*   `ojp-ui`: 前端管理门户。
*   `ojp-jdbc-driver`: 客户端 JDBC 驱动。

### Docker Compose 配置
本项目采用模块化的 Docker Compose 设置：
*   **`docker-compose.yml`**: 主入口文件。包含基础服务 (Kong, 监控等)。
*   **`docker-compose-dev.yml`**: **[仅限开发]** 整合后的开发环境。包含：
    *   **数据库**: MySQL 8/5.7, StarRocks, Redis。
    *   **代理 (Proxies)**: `socat` 代理服务 (`a8-dbproxy`, `a8-ui`)，将流量转发到宿主机端口 (便于 IDE 调试)。
    *   **辅助组件**: Oracle XE, Windows 7。
*   **`docker-compose-sidecar.yml`**: `ojp-sql-translator` Sidecar 的共享定义。
*   **`docker-compose-a8-dbproxy.yml`**: **[生产/全容器化]** 运行容器化 `a8-dbproxy` 和 `a8-ui` 的定义 (不使用本地代理)。

## 3. 快速开始 (开发环境)

### 前置要求
*   Java 22 (推荐 Zulu/Temuri)
*   Maven / `mvnd`
*   Node.js (前端构建)
*   Docker Desktop / Compose
*   Python 3.9+ (如果需要本地调试 Sidecar)

### 启动环境
启动完整的开发基础设施 (数据库 + 代理 + Sidecar)：
```bash
docker-compose up -d
```
*   **a8-dbproxy 代理**: 监听 `172.24.0.40:8010` -> 转发至宿主机 `8010`
*   **a8-ui 代理**: 监听 `172.24.0.41:5173` -> 转发至宿主机 `5173`
*   **SQL Translator**: 作为容器 `ojp-sql-translator` 运行，暴露在 `a8` 网络中。

### 构建与运行服务
1.  **后端 (a8-dbproxy)**:
    ```bash
    cd ojp
    mvn clean package -pl ojp-server -am -DskipTests
    # 运行主类: org.openjdbcproxy.server.OjpServerApplication
    ```
2.  **前端 (a8-ui)**:
    ```bash
    cd ojp/ojp-ui
    npm install
    npm run dev
    ```

## 4. 服务门户与端口表
| 服务 | 宿主机端口 | 内部 IP (a8 net) | 描述 |
| :--- | :--- | :--- | :--- |
| **Kong Gateway** | `8000` | `172.24.0.30` | 统一入口网关 |
| **Grafana** | `3000` | `172.24.0.4` | 监控仪表盘 |
| **Prometheus** | `9090` | `172.24.0.3` | 指标采集 |
| **MySQL 8** | `3306` | `172.24.0.10` | 业务源库 |
| **StarRocks FE** | `9030` | `172.24.0.13` | OLAP / 缓存存储 |
| **Redis** | `6379` | `172.24.0.5` | 缓存元数据 |
| **a8-dbproxy (Dev)** | `8010` (Host) | `172.24.0.40` | OJP Server (代理) |
| **a8-ui (Dev)** | `5173` (Host) | `172.24.0.41` | 管理 UI (代理) |
| **SQL Translator**| N/A | `ojp-sql-translator` | 内部 Sidecar |

## 5. 开发指南
*   **Java**: 使用 Lombok。包名规范 `org.openjdbcproxy.*`。
*   **配置**: 配置文件统一放置在 `src/main/resources`。
*   **构建**: 使用 `mvnd install -DskipTests` 加速构建。
*   **SQL 翻译**: 逻辑位于 `ojp-sql-translator` (FastAPI)。`a8-dbproxy` 通过 HTTP 调用它，将 Oracle SQL 转换为 StarRocks 语法以实现缓存。

## 6. 维护说明
*   **重命名**: 若需重命名服务，请同步更新 `docker-compose-dev.yml` (代理) 和 `docker-compose-a8-dbproxy.yml` (真实容器)。
*   **日志**: 使用 `docker-compose logs -f [service_name]` 查看日志。