---
inclusion: ojp项目
---

# OJP (Open JDBC Proxy) Architecture Guide

## Project Overview
OJP is a smart database middleware providing transparent caching, slow query isolation, and data synchronization. It acts as a proxy between applications and databases, intercepting JDBC calls to provide intelligent caching and query optimization.

## Core Architecture Components

### Backend Modules (`ojp/`)
- **`ojp-server`**: Core gRPC server (port 1059) - intercepts JDBC calls, handles caching logic
- **`ojp-jdbc-driver`**: Custom JDBC driver that connects to ojp-server instead of database directly
- **`ojp-cache`**: Redis-based smart caching layer with intelligent cache decision logic
- **`ojp-grpc-commons`**: Shared Protobuf definitions for driver-server communication
- **`ojp-server-common`**: Shared utilities and common components
- **`ojp-sync`**: Data synchronization components for CDC operations
- **`shopservice`**: Demo microservice for testing and validation

### Frontend
- **`ojp-ui`**: React/Vite dashboard (dev: 5173, prod: 50080) for monitoring and management

## Technology Stack Standards

### Java Backend (Java 22)
- **Framework**: Spring Boot 3.3.3 with gRPC integration
- **Package naming**: `org.openjdbcproxy.*` aligned with module structure
- **Code style**: 4-space indentation, Lombok for boilerplate reduction
- **Configuration**: YAML files in `src/main/resources`
- **Testing**: JUnit 5 with Mockito, package structure mirrors `src/main`
- **Build tool**: Maven with `mvnd` for faster builds

### Frontend (React)
- **Framework**: React 18 with Vite build system
- **Code style**: 2-space indentation, single quotes, functional components
- **File naming**: PascalCase for components
- **Shared utilities**: Place in `src/services` or `src/config`

## Infrastructure & Service Mesh

### Container Network (172.24.0.0/16)
- **Gateway**: Kong (8000 proxy, 8001 admin) at 172.24.0.30
- **Databases**: MySQL (3306), Redis (6379), StarRocks (9030/8030)
- **Monitoring**: Prometheus (9090), Grafana (3000)
- **CDC**: SeaTunnel Zeta cluster for data synchronization

### Docker Compose Structure
- `docker-compose.yml`: Base services and monitoring
- `docker-compose-db.yml`: Database services
- `docker-compose-ojp.yml`: OJP application services
- `docker-compose-cdc-sync-zeta.yml`: CDC and data sync services
- `start-dev.bat`: Development environment orchestration script

## Development Workflow

### Build Commands
```bash
# Backend build (from ojp/)
mvnd install -DskipTests

# Frontend development (from ojp/ojp-ui/)
npm run dev

# Full environment startup
start-dev.bat [-build|-clean|-logs|-compile]
```

### Testing Strategy
- **Backend**: JUnit 5 tests with behavior-driven naming (e.g., `shouldHandleBulkConnect`)
- **Frontend**: Manual validation required - ensure `npm run lint` and `npm run build` pass
- **Integration**: Use compose profiles for dependency management

## Key Architectural Patterns

### Smart Caching Logic
- Cache decisions based on query patterns and data characteristics
- Redis-based storage with intelligent eviction policies
- Transparent to application layer - no code changes required

### gRPC Communication
- Driver-server communication via gRPC for performance
- Protobuf definitions in `ojp-grpc-commons`
- Exception propagation from server to client drivers

### CDC Integration
- SeaTunnel-based change data capture
- MySQL → StarRocks synchronization
- NATS messaging for event streaming

## Configuration Management

### Environment Variables
- Database connections via environment variables
- Redis configuration through `REDIS_HOST`/`REDIS_PORT`
- Credentials in external `.env` files (not version controlled)

### Service Discovery
- Kong gateway for service routing
- Internal service communication via container names
- Health checks for service readiness

## Monitoring & Observability

### Metrics Collection
- Prometheus metrics export from all services
- Grafana dashboards for visualization
- OpenTelemetry instrumentation for tracing

### Logging Standards
- Structured logging with SLF4J/Logback
- Log aggregation through container volumes
- Service-specific log files in `logs/` directories

## Common Development Tasks

### Adding New Features
1. Update Protobuf definitions in `ojp-grpc-commons` if needed
2. Implement server-side logic in `ojp-server`
3. Update JDBC driver in `ojp-jdbc-driver` if client changes needed
4. Add UI components in `ojp-ui` for management interfaces

### Database Integration
- Support for MySQL, PostgreSQL, H2, MariaDB
- StarRocks for OLAP workloads
- Connection pooling via HikariCP

### Performance Considerations
- LZ4 compression for data transfer
- Connection pooling and reuse
- Intelligent query routing based on cache hit rates

## Troubleshooting Guidelines

### Common Issues
- **gRPC connectivity**: Check port 1059 availability and network configuration
- **Cache misses**: Verify Redis connectivity and cache decision logic
- **CDC lag**: Monitor SeaTunnel job status and NATS message queues
- **Frontend proxy**: Ensure backend services are healthy before UI startup

### Debug Tools
- Kong Admin API for gateway debugging
- Grafana dashboards for performance metrics
- Container logs via `docker-compose logs`
- Health check endpoints on all services