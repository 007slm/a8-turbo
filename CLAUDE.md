# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is **Open JDBC Proxy (OJP)** - a JDBC driver and Layer 7 proxy server that decouples applications from relational database connection management. It provides intelligent connection pooling and supports elastic scaling without overwhelming databases.

## Architecture
- **ojp-server**: gRPC server managing HikariCP connection pools, acts as smart proxy
- **ojp-jdbc-driver**: JDBC driver implementation that connects via gRPC to ojp-server
- **ojp-grpc-commons**: Shared gRPC contracts between server and driver
- **ojp-ui**: Management UI for monitoring and configuring the JDBC proxy service
- Spring Boot + Spring gRPC for dual protocol support (HTTP + gRPC on port 8010)

## Build System
- **Maven** with Java 22
- **Main artifact**: `ojp-server` (port 8010)
- **Packaging**: Spring Boot JAR with embedded server

## Key Commands

### Build & Run
```bash
# Build all modules
mvn clean install -DskipTests

# Run OJP server only
mvn spring-boot:run -pl ojp-server

# Run management UI
cd ojp-ui && npm install && npm run dev

# Build and run with Docker
docker run --rm -d -p 8010:8010 rrobetti/ojp:0.0.8-alpha

# Build fat JAR for standalone
mvn package -pl ojp-server
java -jar ojp/ojp-server/target/ojp-server-0.0.8-alpha.jar
```

### Development
```bash
# Start infrastructure services
docker-compose up -d prometheus grafana redis phpredisadmin

# Run tests (requires running server)
mvn test -pl ojp-jdbc-driver -DdisablePostgresTests -DdisableMySQLTests -DdisableMariaDBTests

# Run specific database tests
mvn test -pl ojp-jdbc-driver -DdisableH2Tests -DdisablePostgresTests
```

## Project Structure
```
├── ojp/
│   ├── ojp-server/           # Main gRPC server (Spring Boot)
│   ├── ojp-jdbc-driver/      # JDBC driver implementation
│   ├── ojp-grpc-commons/     # Shared gRPC contracts
│   ├── ojp-ui/               # Management UI (React-based)
│   └── documents/            # Architecture docs & guides
├── docker/                   # Docker compose configs
├── docker-compose.yml        # Main services (Prometheus, Grafana, Redis)
└── smart--redis-cache/       # Smart caching implementation
```

## Core Components

### gRPC Services
- **StatementServiceImpl**: Main SQL execution service
- **ConnectionServiceImpl**: Connection lifecycle management
- **MetadataServiceImpl**: Database metadata operations

### Management UI (ojp-ui)
- **Technology**: React-based web application
- **Purpose**: Monitor and configure the JDBC proxy service
- **Features**: Connection pool monitoring, cache management, query statistics
- **Development**: Standard React development workflow

### Interceptors (Chain Pattern)
- **SmartCacheInterceptor**: Intelligent caching layer
- **SlowQueryLoggingInterceptor**: Performance monitoring
- **CircuitBreakerInterceptor**: Fault tolerance
- **GlobalLoggingInterceptor**: Request/response logging

### Cache System
- **CacheRuleEngine**: Rule-based caching decisions
- **CacheKeyGenerator**: Consistent cache key generation
- **MetricsCollector**: Cache performance monitoring

## Configuration
- **Server port**: 8010 (HTTP + gRPC unified)
- **Health check**: http://localhost:8010/api/actuator/health
- **Metrics**: http://localhost:59050/metrics (Prometheus)
- **Redis**: localhost:6379 (for cache & statistics)
- **Grafana**: http://localhost:3000 (admin/fluxDB)
- **Management UI**: http://localhost:3001 (ojp-ui default)

## Smart Cache Features
- SQL query caching with configurable rules
- Automatic cache invalidation
- Statistics collection via Redis
- REST API endpoints for cache management
- Integration with StarRocks for analytics

## Development Setup
1. Java 22+ and Maven 3.9+
2. Node.js 18+ for ojp-ui development
3. Docker for infrastructure services
4. Disable application-level connection pools when using OJP
5. Update JDBC URLs with `ojp[host:port]_` prefix

## Testing
- Integration tests for: H2, PostgreSQL, MySQL, MariaDB, Oracle, SQL Server
- Test configurations in CSV files under test/resources/
- Database-specific flags: `-DdisableH2Tests`, `-DenableOracleTests`

## Key URLs & Endpoints
- **Server**: http://localhost:8010
- **gRPC**: localhost:8010 (unified with HTTP)
- **Metrics**: http://localhost:59050/metrics
- **Grafana**: http://localhost:3000
- **Management UI**: http://localhost:3001
- **Redis Admin**: http://localhost:8181