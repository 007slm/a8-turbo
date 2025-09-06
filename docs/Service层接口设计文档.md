# Service层接口设计文档

## 概述

Service层是FluxDB系统的核心业务逻辑层，负责处理缓存规则管理、查询处理、统计数据维护等核心功能。本文档定义了Service层的接口规范，包括管理类接口（支持RESTful API）和运行时操作接口（支持JDBC驱动运行时调用）。FluxDB是一个基于Redis + StarRocks双层存储架构的查询路由系统。

## 架构说明

### 服务分层
- **Controller层**: RESTful API控制器，调用Service层接口
- **Service层**: 核心业务逻辑，本文档定义的接口层
- **Repository层**: 数据访问层，操作Redis数据结构
- **Runtime层**: JDBC驱动运行时，调用Service层接口
- **数据层**: StarRocks（OLAP查询）+ MySQL（事务处理）双存储架构

### 接口分类
1. **管理类接口**: 支持RESTful API的CRUD操作
2. **运行时接口**: 支持JDBC驱动的查询路由决策、统计更新等操作

---

## 1. 查询管理服务接口

### 1.1 QueryManagementService

```java
public interface QueryManagementService {
    
    /**
     * 获取查询列表（按数据库分组）
     * @return 按数据库名称分组的查询列表
     */
    Map<String, List<QuerySummary>> getQueries();
    
    /**
     * 获取查询详情
     * @param queryId 查询ID
     * @return 查询详情
     */
    QueryDetailResponse getQueryDetail(String queryId);
    
    /**
     * 注册新查询（运行时调用）
     * @param datasourceName 数据库名称
     * @param query 查询对象
     */
    void registerQuery(String datasourceName, Query query);
    
    /**
     * 更新查询统计信息（运行时调用）
     * @param datasourceName 数据库名称
     * @param queryId 查询ID
     * @param executionTime 执行时间（毫秒）
     * @param cacheHit 是否缓存命中
     */
    void updateQueryStats(String datasourceName, String queryId, long executionTime, boolean cacheHit);
}
```

### 1.2 数据传输对象

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuerySummary {
    private String queryId;
    private String datasourceName;
    private String sql;
    private Set<String> tables;
    private long accessCount;
    private double meanQueryTime;
    private String lastAccess;
    private boolean cached;
    private double cacheHitRate;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryDetailResponse {
    private String queryId;
    private String datasourceName;
    private String sql;
    private Set<String> tables;
    private long accessCount;
    private double meanQueryTime;
    private double maxQueryTime;
    private double minQueryTime;
    private String lastAccess;
    private boolean cached;
    private double cacheHitRate;
    private RuleInfo currentRule;
    private CacheStatus cacheStatus;
}
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatus {
    private String cacheKey;
    private int ttlRemaining;
    private long cacheSize;
    private String lastCacheUpdate; // ISO格式: "2024-01-01T12:00:00Z"
}
```

---

## 2. 规则管理服务接口

### 2.1 RuleManagementService

```java
public interface RuleManagementService {
    
    /**
     * 获取缓存规则列表（按数据库分组）
     * @return 按数据库名称分组的规则列表
     */
    Map<String, List<CacheRuleResponse>> getRules();
    
    /**
     * 创建缓存规则
     * @param request 创建请求
     * @return 创建的规则
     */
    CacheRuleResponse createRule(CreateCacheRuleRequest request);
    
    /**
     * 更新缓存规则
     * @param ruleId 规则ID
     * @param request 更新请求
     * @return 更新后的规则
     */
    CacheRuleResponse updateRule(String ruleId, UpdateCacheRuleRequest request);
    
    /**
     * 删除缓存规则
     * @param ruleId 规则ID
     */
    void deleteRule(String ruleId);
    
    /**
     * 获取规则配置（运行时调用）
     * @param datasourceName 数据库名称
     * @return 规则配置数组
     */
    RuleConfig[] getRuleConfigs(String datasourceName);
    
    /**
     * 匹配查询规则（运行时调用）
     * @param datasourceName 数据库名称
     * @param query 查询对象
     * @return 匹配的动作（包含TTL等信息）
     */
    Action matchRule(String datasourceName, Query query);
}
```

### 2.2 数据传输对象

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCacheRuleRequest {
    private String ruleName;
    private String datasourceName;
    private String dbType;
    private List<String> tables;
    private String ruleType; // tables, tablesAny, tablesAll, queryIds, regex
    private int ttl; // 秒数
    private boolean enabled;
    private String description;
    private int priority;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCacheRuleRequest {
    private String ruleName;
    private String dbType;
    private List<String> tables;
    private String ruleType; // tables, tablesAny, tablesAll, queryIds, regex
    private int ttl; // 秒数
    private boolean enabled;
    private String description;
    private int priority;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheRuleResponse {
    private String ruleId;
    private String ruleName;
    private String datasourceName;
    private String dbType;
    private List<String> tables;
    private String ruleType; // tables, tablesAny, tablesAll, queryIds, regex
    private int ttl; // 秒数
    private boolean enabled;
    private String description;
    private int priority;
    private String createdAt; // ISO格式: "2024-01-01T12:00:00Z"
    private String updatedAt; // ISO格式: "2024-01-01T12:00:00Z"
    private List<String> matchedQueries;
}
```

---

## 3. 统计管理服务接口

### 3.1 StatisticsManagementService

```java
public interface StatisticsManagementService {
    
    /**
     * 获取表格统计列表（按数据库分组）
     * @return 按数据库名称分组的表格统计列表
     */
    Map<String, List<TableStatsSummary>> getTableStats();
    
    /**
     * 获取表格详细统计
     * @param tableName 表名
     * @return 表格详细统计
     */
    TableStatsDetail getTableStatsDetail(String tableName);
    
    /**
     * 更新表格统计（运行时调用）
     * @param datasourceName 数据库名称
     * @param tableName 表名
     * @param cacheHit 是否缓存命中
     * @param executionTime 执行时间
     */
    void updateTableStats(String datasourceName, String tableName, boolean cacheHit, long executionTime);
}
```

### 3.2 数据传输对象

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatsSummary {
    private String name;
    private String datasourceName;
    private long accessFrequency;
    private double avgQueryTime;
    private double cacheHitRate;
    private RuleInfo currentRule;
    private List<String> relatedQueries;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatsDetail {
    private String name;
    private String datasourceName;
    private long accessFrequency;
    private double avgQueryTime;
    private double maxQueryTime;
    private double minQueryTime;
    private double cacheHitRate;
    private long totalCacheSize;
    private RuleInfo currentRule;
    private List<RelatedQuery> relatedQueries;
    private List<PerformanceHistory> performanceHistory;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedQuery {
    private String queryId;
    private long accessCount;
    private double avgTime;
    private boolean cached;
}
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceHistory {
    private String timestamp; // ISO格式: "2024-01-01T12:00:00Z"
    private double avgQueryTime;
    private long accessCount;
    private double cacheHitRate;
}
```

---

## 4. 缓存决策服务接口

### 4.1 CacheDecisionService

```java
public interface CacheDecisionService {
    
    /**
     * 判断查询是否应该走缓存路径（查询StarRocks）
     * @param datasourceName 数据库名称
     * @param sql SQL语句
     * @param parameters 查询参数
     * @return 缓存决策结果
     */
    CacheDecision shouldUseCache(String datasourceName, String sql, Object[] parameters);
    
    /**
     * 清理表级缓存配置（触发重新同步）
     * @param datasourceName 数据库名称
     * @param tableName 表名
     */
    void clearTableCache(String datasourceName, String tableName);
}

### 4.2 数据传输对象

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDecision {
    private boolean useCache;
    private String reason;
    private String ruleId;
    private int ttl;
}
```



---

## 5. 运行时集成服务接口

### 5.1 RuntimeIntegrationService

```java
public interface RuntimeIntegrationService {
    
    /**
     * 处理查询路由（JDBC驱动调用）
     * 根据缓存配置决定查询StarRocks还是MySQL
     * @param datasourceName 数据库名称
     * @param sql SQL语句
     * @param parameters 查询参数
     * @param starRocksExecutor StarRocks查询执行器
     * @param mysqlExecutor MySQL查询执行器
     * @return 查询结果
     */
    <T> T routeQuery(String datasourceName, String sql, Object[] parameters, 
                    QueryExecutor<T> starRocksExecutor, QueryExecutor<T> mysqlExecutor);
    
    /**
     * 记录查询统计信息（JDBC驱动调用）
     * @param datasourceName 数据库名称
     * @param queryId 查询ID
     * @param executionTime 执行时间
     * @param cacheHit 是否缓存命中（即是否查询StarRocks）
     */
    void recordQueryStats(String datasourceName, String queryId, long executionTime, boolean cacheHit);
    
    /**
     * 获取查询规则会话（JDBC驱动调用）
     * @param datasourceName 数据库名称
     * @return 规则会话
     */
    QueryRuleSession getRuleSession(String datasourceName);
    
    /**
     * 记录查询信息
     * @param datasourceName 数据库名称
     * @param queryInfo 查询信息
     */
    void recordQuery(String datasourceName, QueryInfo queryInfo);
    
    /**
     * 获取缓存决策
     * @param datasourceName 数据库名称
     * @param sql SQL语句
     * @param tables 涉及的表
     * @return 缓存决策
     */
    CacheDecision getCacheDecision(String datasourceName, String sql, Set<String> tables);
}
```

### 5.2 辅助接口

```java
@FunctionalInterface
public interface QueryExecutor<T> {
    T execute() throws Exception;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryInfo {
    private String queryId;
    private String datasourceName;
    private String sql;
    private Set<String> tables;
    private List<String> parameters;
    private long executionTime;
    private String timestamp; // ISO格式: "2024-01-01T12:00:00Z"
}
```

---

## 6. 异常定义

**技术要求**：
- 采用 Lombok 简化异常类定义，使用 `@Getter`、`@RequiredArgsConstructor` 等注解
- 减少样板代码，提高代码可读性和维护性

```java
public class CacheServiceException extends RuntimeException {
    public CacheServiceException(String message) {
        super(message);
    }
    
    public CacheServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

@Getter
public class RuleNotFoundException extends CacheServiceException {
    private final String ruleId;
    
    public RuleNotFoundException(String ruleId) {
        super("Rule not found: " + ruleId);
        this.ruleId = ruleId;
    }
}

@Getter
public class QueryNotFoundException extends CacheServiceException {
    private final String queryId;
    
    public QueryNotFoundException(String queryId) {
        super("Query not found: " + queryId);
        this.queryId = queryId;
    }
}

@Getter
public class DatabaseNotInitializedException extends CacheServiceException {
    private final String datasourceName;
    
    public DatabaseNotInitializedException(String datasourceName) {
        super("Database not initialized: " + datasourceName);
        this.datasourceName = datasourceName;
    }
}
```

---

## 7. 实现要点

### 7.1 架构理解
- **不是传统缓存系统**：系统不将查询结果存储到Redis中
- **查询路由系统**：根据配置决定查询StarRocks（缓存命中）还是MySQL（缓存未命中）
- **Redis作用**：仅存储配置信息和统计数据，不存储查询结果
- **数据同步**：通过Flink CDC将MySQL数据同步到StarRocks

### 7.2 接口设计原则
- 遵循RESTful API文档的响应格式，确保Controller层直接调用
- 去除复杂的分页参数，缓存规则数量有限（最多千条），全量返回
- 按数据库名称自然分组，简化数据结构
- **Lombok 使用**：采用 Lombok 注解简化模板代码，减少样板代码，提高开发效率

### 7.3 查询路由逻辑
- 根据缓存规则配置判断查询是否走"缓存路径"（StarRocks）
- 缓存命中 = 查询StarRocks（高性能OLAP）
- 缓存未命中 = 查询MySQL（传统关系型数据库）
- 统计信息记录到Redis用于监控和分析

### 7.4 性能考虑
- 统计数据更新采用异步方式，避免影响查询性能
- 规则匹配结果可以缓存，减少重复计算
- 全量数据返回，避免复杂的分页逻辑

---

## 8. 工具类设计

### 8.1 datasourceNameGenerator

数据库名称生成工具类，负责从JDBC URL中提取并生成标准化的数据库名称。

**技术要求**：
- 采用 Lombok 简化模板代码，使用 `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor` 等注解
- 减少样板代码，提高代码可读性和维护性

```java
public class datasourceNameGenerator {
    
    /**
     * 从JDBC URL生成数据库名称
     * @param jdbcUrl JDBC连接字符串
     * @return 标准化的数据库名称，格式：{dbType}_{host}_{port}_{username}_{dbname}_db
     * @throws IllegalArgumentException 当URL格式不正确时
     */
    public static String generatedatasourceName(String jdbcUrl) {
        // 实现逻辑见下方规则说明
    }
    
    /**
     * 从JDBC URL和用户名生成数据库名称
     * @param jdbcUrl JDBC连接字符串
     * @param username 用户名（当URL中不包含用户名时使用）
     * @return 标准化的数据库名称
     */
    public static String generatedatasourceName(String jdbcUrl, String username) {
        // 支持外部传入用户名的重载方法
    }
    
    // 内部辅助方法：isValiddatasourceName() 和 parseJdbcUrl() 等
    // 这些方法为内部实现细节，不对外暴露
}

/**
 * JDBC URL解析结果数据结构
 * 采用 Lombok 注解简化数据类定义，自动生成 getter/setter、构造函数、equals/hashCode 等方法
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class DatabaseUrlInfo {
    private String databaseType;    // 数据库类型：mysql, postgresql, oracle, sqlserver
    private String host;           // 主机地址
    private int port;              // 端口号
    private String username;       // 用户名
    private String datasourceName;   // 数据库名称
    private String schema;         // 模式名（Oracle等）
}
```

#### 8.1.1 生成规则

**基本规则：**
1. 从JDBC URL中提取数据库类型、主机地址、端口、用户名、数据库名称
2. 按照固定格式组合：`{dbType}_{host}_{port}_{username}_{dbname}_db`
3. 转换为小写并替换所有特殊字符为下划线
4. 确保名称符合Redis键命名规范（仅包含字母、数字、下划线）

**支持的JDBC URL格式：**
```
# MySQL
jdbc:mysql://localhost:3306/ecommerce?user=root
→ mysql_localhost_3306_root_ecommerce_db

jdbc:mysql://192.168.1.100:3306/user-service?user=app_user
→ mysql_192_168_1_100_3306_app_user_user_service_db

# PostgreSQL  
jdbc:postgresql://db.example.com:5432/inventory?user=postgres
→ postgresql_db_example_com_5432_postgres_inventory_db

# Oracle
jdbc:oracle:thin:scott/tiger@localhost:1521:XE
→ oracle_localhost_1521_scott_xe_db

# SQL Server
jdbc:sqlserver://sqlserver.local:1433;datasourceName=orders;user=sa
→ sqlserver_sqlserver_local_1433_sa_orders_db
```

**特殊字符处理：**
- 点号(.) → 下划线(_)
- 连字符(-) → 下划线(_)
- 冒号(:) → 下划线(_)
- 斜杠(/) → 下划线(_)
- 空格 → 下划线(_)
- 其他特殊字符 → 移除
- 重复的下划线 → 合并为单个下划线

**边界情况处理：**
- URL中无主机地址：使用"localhost"
- URL中无端口：使用数据库默认端口（MySQL:3306, PostgreSQL:5432等）
- URL中无用户名：使用"default_user"
- URL中无数据库名称：使用"default_schema"
- 生成的名称过长：截取各部分并保持总长度不超过100字符
- IP地址格式：将点号替换为下划线（192.168.1.100 → 192_168_1_100）

#### 8.1.2 使用场景

1. **JDBC驱动初始化**：从连接URL自动生成数据库名称
2. **配置验证**：验证手动配置的数据库名称格式
3. **Redis键生成**：为Redis数据结构提供标准化的数据库标识
4. **日志记录**：统一的数据库标识用于日志和监控

#### 8.1.3 集成说明

- **Service层调用**：所有Service接口在接收datasourceName参数前进行验证
- **JDBC驱动集成**：SmartConnection初始化时自动调用生成方法
- **配置管理**：支持手动指定datasourceName，但需通过验证
- **Redis集成**：生成的datasourceName直接用于Redis键前缀

---

## 9. 与现有组件的集成

### 9.1 与Redis数据结构的映射
- 查询信息存储：`{datasourceName}:query:{queryId}`
- 规则配置存储：`{datasourceName}:rules`
- 统计数据存储：`{datasourceName}:stats:*`
- 缓存数据存储：`{datasourceName}:cache:{queryId}:*`

### 9.2 与JDBC驱动的集成
- `SmartConnection`调用`RuntimeIntegrationService`
- `SmartStatement`调用`CacheOperationService`和统计更新接口
- 查询规则匹配通过`RuleManagementService`
- `datasourceNameGenerator`在连接初始化时自动生成数据库名称

### 9.3 与RESTful API的集成
- Controller层直接调用管理类Service接口
- 数据传输对象与API响应格式一致
- 异常处理统一转换为HTTP状态码
- `datasourceNameGenerator`用于验证API请求中的数据库名称参数

---

## 总结

本Service层接口设计基于FluxDB系统架构的正确理解，严格遵循已审核的RESTful API文档和Redis数据结构文档：

### 核心功能模块：
1. **查询管理**: 支持查询列表获取（按数据库分组）、查询详情、运行时查询注册和统计更新
2. **规则管理**: 支持规则CRUD操作（去除复杂参数）、运行时规则匹配
3. **统计管理**: 支持表格统计数据收集和查询（按数据库分组）
4. **缓存决策**: 支持查询路由决策，判断是否走StarRocks路径
5. **运行时集成**: 支持JDBC驱动的查询路由、统计记录和规则会话管理

### 架构特点：
- **查询路由系统**: 不是传统Redis缓存，而是基于配置的查询路由
- **双数据源**: 缓存命中查询StarRocks，未命中查询MySQL
- **Redis角色**: 仅存储配置和统计信息，不存储查询结果
- **数据同步**: 通过Flink CDC实现MySQL到StarRocks的数据同步

### 设计特点：
- **简化参数**: 去除复杂的分页参数，缓存规则数量有限，全量返回
- **自然分组**: 按数据库名称自然分组，简化数据结构
- **完全一致**: 数据传输对象与RESTful API响应格式完全一致
- **直接对接**: Controller层可以直接调用Service接口，无需额外转换

接口设计遵循单一职责原则，每个Service专注于特定的业务领域，准确反映了FluxDB系统的查询路由本质。