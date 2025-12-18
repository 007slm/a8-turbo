# A8 Turbo - Project Knowledge Base

> **Generated on:** 2025-12-17
> **Context:** Auto-generated summaries of project architecture, infrastructure, and development norms.

## 1. Project Overview
**A8 Turbo** is an advanced integration environment for the **Open-JDBC-Proxy (OJP)** project. It is designed to act as a smart database middleware layer providing Transparent Caching, Slow Query Isolation, and Data Synchronization.

### Core Technology Stack
- **Backend:** Java 22, Spring Boot 3.3.3, gRPC
- **Frontend:** React, Vite (located in `ojp/ojp-ui`)
- **Data Stores:** MySQL (Source), StarRocks (OLAP), Redis (Cache)
- **Integration:** SeaTunnel (CDC/Sync), Kong (Gateway)
- **Monitoring:** Prometheus, Grafana

## 2. Module Structure (`ojp/`)

| Module | Description |
| :--- | :--- |
| **`ojp-server`** | The core gRPC server. Intercepts JDBC calls, handles caching logic, and communicates with the database. |
| **`ojp-jdbc-driver`** | A custom JDBC driver that connects to `ojp-server` instead of the database directly. |
| **`ojp-cache`** | Manages Redis interactions for the "Smart Cache" feature. |
| **`ojp-grpc-commons`** | Shared Protobuf definitions defining the contract between Driver and Server. |
| **`ojp-sync`** | Components for data synchronization logic. |
| **`shopservice`** | A demo microservice used to generate traffic and validate OJP features. |

## 3. Infrastructure & Ports
The project relies heavily on Docker Compose. Refer to `PORTAL.md` for the single source of truth.

### Key Service Ports
- **OJP Server:** `1059`
- **OJP UI:** `5173` (Dev), `50080` (Prod)
- **Kong Gateway:** `8000` (Proxy), `8001` (Admin)
- **StarRocks:** `9030` (FE MySQL Port), `8030` (FE HTTP)
- **SeaTunnel Master:** `8080`
- **Grafana:** `3000`

### Docker Compose Files
- `docker-compose.yml`: Base services (Kong, Monitoring).
- `docker-compose-db.yml`: Storage (MySQL, Redis, StarRocks).
- `docker-compose-ojp.yml`: OJP application services.
- `docker-compose-cdc-sync-zeta.yml`: CDC and SeaTunnel clusters.

## 4. Development Guidelines (from `AGENTS.md`)
- **Java:**
    - Use Lombok.
    - Package naming: `org.openjdbcproxy.*`.
    - Configuration in `src/main/resources` YAML files.
- **Frontend:**
    - PascalCase for components.
    - 2-space indentation.
- **Build:**
    - Use `mvnd install -DskipTests` for fast builds.
    - Use `start-dev.bat` to manage the local environment.

## 5. Current Active Context
- **StarRocks Compatibility:** Recent updates focus on handling Composite Primary Keys in StarRocks sinks using OJP caching.
- **CDC Stability:** Fixes applied to `MySQL-CDC` connectors in SeaTunnel.
- **Bug Fixes:** 
    - DB2 cursor compatibility.
    - LOB streaming concurrency (Switch to ReentrantLock).
    - Improved gRPC exception propagation to client drivers.
