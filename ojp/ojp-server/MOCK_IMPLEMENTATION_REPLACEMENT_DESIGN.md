# OJP Server 模拟实现替换设计方案

## 概述

本文档详细描述了如何将 OJP Server 中发现的模拟实现、临时代码和简化实现替换为真实的生产级实现。这些改进将显著提升系统的可靠性、性能和可维护性。

## 发现的问题总结

### 1. CacheStatsController.java - 8个临时实现
- 查询列表信息返回空Map
- 特定查询的缓存规则返回空数据
- 为特定查询创建缓存规则的逻辑未实现
- 表格列表信息返回空Map
- 特定表格的缓存规则返回空数据
- 为特定表格创建缓存规则的逻辑未实现
- 特定表格的统计信息未实现
- 查询统计信息仅用于测试和模拟数据

### 2. ServerController.java - 模拟服务器数据
- 使用内存中的ConcurrentHashMap存储服务器信息
- 缺少真实的数据库持久化
- 服务器状态管理不完整

### 3. SystemSettingsController.java - 模拟系统配置数据
- 使用内存中的ConcurrentHashMap存储配置
- 缺少配置持久化机制
- 配置验证逻辑不完整

## 设计方案

### 阶段一：数据层重构

#### 1.1 数据库设计

**1.1.1 服务器管理表**
```sql
-- 服务器信息表
CREATE TABLE servers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    database_type VARCHAR(50),
    username VARCHAR(100),
    password_encrypted VARCHAR(255),
    status VARCHAR(20) DEFAULT 'stopped',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_heartbeat TIMESTAMP,
    connection_pool_size INT DEFAULT 10,
    max_connections INT DEFAULT 100,
    timeout_ms INT DEFAULT 30000,
    is_active BOOLEAN DEFAULT TRUE
);

-- 服务器状态历史表
CREATE TABLE server_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (server_id) REFERENCES servers(id)
);
```

**1.1.2 缓存规则表**
```sql
-- 缓存规则表
CREATE TABLE cache_rules (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type ENUM('TABLES', 'QUERY', 'REGEX') NOT NULL,
    pattern TEXT NOT NULL,
    ttl_seconds INT NOT NULL,
    priority INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    description TEXT
);

-- 缓存规则应用历史表
CREATE TABLE cache_rule_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(50) NOT NULL,
    query_hash VARCHAR(64),
    table_name VARCHAR(100),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cache_hit BOOLEAN,
    response_time_ms INT,
    FOREIGN KEY (rule_id) REFERENCES cache_rules(id)
);
```

**1.1.3 系统配置表**
```sql
-- 系统配置表
CREATE TABLE system_configs (
    id VARCHAR(100) PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    key_name VARCHAR(100) NOT NULL,
    value TEXT,
    value_type ENUM('STRING', 'INTEGER', 'BOOLEAN', 'JSON') DEFAULT 'STRING',
    description TEXT,
    is_sensitive BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_key (category, key_name)
);

-- 配置变更历史表
CREATE TABLE config_change_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    FOREIGN KEY (config_id) REFERENCES system_configs(id)
);
```

**1.1.4 性能统计表**
```sql
-- 查询性能统计表
CREATE TABLE query_performance_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_hash VARCHAR(64) NOT NULL,
    query_text TEXT,
    table_name VARCHAR(100),
    execution_count BIGINT DEFAULT 0,
    total_execution_time_ms BIGINT DEFAULT 0,
    avg_execution_time_ms DECIMAL(10,2) DEFAULT 0,
    max_execution_time_ms INT DEFAULT 0,
    min_execution_time_ms INT DEFAULT 0,
    cache_hit_count BIGINT DEFAULT 0,
    cache_miss_count BIGINT DEFAULT 0,
    last_executed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_query_hash (query_hash),
    INDEX idx_table_name (table_name),
    INDEX idx_last_executed (last_executed_at)
);

-- 缓存命中率统计表
CREATE TABLE cache_hit_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(50),
    table_name VARCHAR(100),
    date DATE NOT NULL,
    hour INT NOT NULL,
    total_requests BIGINT DEFAULT 0,
    cache_hits BIGINT DEFAULT 0,
    cache_misses BIGINT DEFAULT 0,
    hit_rate DECIMAL(5,2) DEFAULT 0,
    avg_response_time_ms DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_date_hour (rule_id, date, hour),
    INDEX idx_table_date (table_name, date)
);
```

#### 1.2 数据访问层 (DAO/Repository)

**1.2.1 服务器管理 Repository**
```java
@Repository
public interface ServerRepository extends JpaRepository<Server, String> {
    List<Server> findByStatus(String status);
    List<Server> findByIsActiveTrue();
    Optional<Server> findByHostAndPort(String host, int port);
    
    @Query("SELECT s FROM Server s WHERE s.lastHeartbeat < :threshold")
    List<Server> findServersWithStaleHeartbeat(@Param("threshold") LocalDateTime threshold);
}
```

**1.2.2 缓存规则 Repository**
```java
@Repository
public interface CacheRuleRepository extends JpaRepository<CacheRule, String> {
    List<CacheRule> findByRuleTypeAndIsActiveTrue(RuleType ruleType);
    List<CacheRule> findByTableNameAndIsActiveTrue(String tableName);
    Optional<CacheRule> findByPatternAndRuleType(String pattern, RuleType ruleType);
    
    @Query("SELECT cr FROM CacheRule cr WHERE cr.pattern REGEXP :queryPattern AND cr.isActive = true")
    List<CacheRule> findMatchingRules(@Param("queryPattern") String queryPattern);
}
```

**1.2.3 系统配置 Repository**
```java
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    List<SystemConfig> findByCategory(String category);
    Optional<SystemConfig> findByCategoryAndKeyName(String category, String keyName);
    
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isSensitive = false")
    List<SystemConfig> findNonSensitiveConfigs();
}
```

**1.2.4 性能统计 Repository**
```java
@Repository
public interface QueryPerformanceRepository extends JpaRepository<QueryPerformanceStats, Long> {
    Optional<QueryPerformanceStats> findByQueryHash(String queryHash);
    List<QueryPerformanceStats> findByTableNameOrderByExecutionCountDesc(String tableName);
    
    @Query("SELECT qps FROM QueryPerformanceStats qps WHERE qps.avgExecutionTimeMs > :threshold ORDER BY qps.avgExecutionTimeMs DESC")
    List<QueryPerformanceStats> findSlowQueries(@Param("threshold") double threshold);
    
    @Query("SELECT qps FROM QueryPerformanceStats qps WHERE qps.lastExecutedAt >= :since ORDER BY qps.executionCount DESC")
    List<QueryPerformanceStats> findRecentPopularQueries(@Param("since") LocalDateTime since);
}
```

### 阶段二：服务层重构

#### 2.1 服务器管理服务

**2.1.1 ServerManagementService**
```java
@Service
@Transactional
public class ServerManagementService {
    
    @Autowired
    private ServerRepository serverRepository;
    
    @Autowired
    private ServerStatusHistoryRepository statusHistoryRepository;
    
    @Autowired
    private DatabaseConnectionManager connectionManager;
    
    public List<ServerInfo> getAllServers() {
        return serverRepository.findByIsActiveTrue()
            .stream()
            .map(this::convertToServerInfo)
            .collect(Collectors.toList());
    }
    
    public ServerInfo createServer(CreateServerRequest request) {
        // 验证服务器连接
        validateServerConnection(request);
        
        // 创建服务器记录
        Server server = new Server();
        server.setId(generateServerId());
        server.setName(request.getName());
        server.setHost(request.getHost());
        server.setPort(request.getPort());
        server.setDatabaseType(request.getDatabaseType());
        server.setUsername(request.getUsername());
        server.setPasswordEncrypted(encryptPassword(request.getPassword()));
        server.setStatus("stopped");
        
        Server savedServer = serverRepository.save(server);
        
        // 记录状态变更
        recordStatusChange(savedServer.getId(), "created", "服务器创建成功");
        
        return convertToServerInfo(savedServer);
    }
    
    public ApiResponse<String> startServer(String serverId) {
        Server server = serverRepository.findById(serverId)
            .orElseThrow(() -> new ServerNotFoundException("服务器不存在: " + serverId));
        
        try {
            // 建立数据库连接
            connectionManager.connect(server);
            
            // 更新服务器状态
            server.setStatus("running");
            server.setLastHeartbeat(LocalDateTime.now());
            serverRepository.save(server);
            
            // 记录状态变更
            recordStatusChange(serverId, "started", "服务器启动成功");
            
            return ApiResponse.success("服务器启动成功", "服务器已成功启动");
        } catch (Exception e) {
            server.setStatus("error");
            serverRepository.save(server);
            recordStatusChange(serverId, "error", "服务器启动失败: " + e.getMessage());
            throw new ServerStartException("服务器启动失败", e);
        }
    }
    
    private void validateServerConnection(CreateServerRequest request) {
        // 实现真实的数据库连接验证
        try (Connection conn = DriverManager.getConnection(
                buildJdbcUrl(request), 
                request.getUsername(), 
                request.getPassword())) {
            // 连接成功，验证通过
        } catch (SQLException e) {
            throw new InvalidServerConfigurationException("无法连接到数据库: " + e.getMessage());
        }
    }
}
```

#### 2.2 缓存统计服务

**2.2.1 CacheStatisticsService**
```java
@Service
@Transactional
public class CacheStatisticsService {
    
    @Autowired
    private QueryPerformanceRepository queryPerformanceRepository;
    
    @Autowired
    private CacheHitStatsRepository cacheHitStatsRepository;
    
    @Autowired
    private CacheRuleRepository cacheRuleRepository;
    
    @Autowired
    private SmartCacheEngine cacheEngine;
    
    public CacheOverviewStats getCacheOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime oneDayAgo = now.minusDays(1);
        
        // 获取实时统计
        long totalRequests = getTotalRequests(oneHourAgo, now);
        long cacheHits = getCacheHits(oneHourAgo, now);
        long cacheMisses = getCacheMisses(oneHourAgo, now);
        
        // 计算命中率
        double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
        
        // 获取活跃规则数量
        long activeRules = cacheRuleRepository.countByIsActiveTrue();
        
        // 获取缓存大小
        long cacheSize = cacheEngine.getCacheSize();
        
        return CacheOverviewStats.builder()
            .totalRequests(totalRequests)
            .cacheHits(cacheHits)
            .cacheMisses(cacheMisses)
            .hitRate(hitRate)
            .activeRules(activeRules)
            .cacheSize(cacheSize)
            .build();
    }
    
    public List<QueryPerformanceInfo> getQueryPerformanceStats(
            String tableName, 
            Integer limit, 
            String sortBy) {
        
        List<QueryPerformanceStats> stats;
        
        if (tableName != null) {
            stats = queryPerformanceRepository.findByTableNameOrderByExecutionCountDesc(tableName);
        } else {
            stats = queryPerformanceRepository.findAll(
                PageRequest.of(0, limit != null ? limit : 50, 
                    Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "executionCount"))
            ).getContent();
        }
        
        return stats.stream()
            .map(this::convertToQueryPerformanceInfo)
            .collect(Collectors.toList());
    }
    
    public List<TablePerformanceInfo> getTablePerformanceStats() {
        // 实现真实的表格性能统计
        return queryPerformanceRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(
                QueryPerformanceStats::getTableName,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::calculateTableStats
                )
            ))
            .entrySet()
            .stream()
            .map(entry -> TablePerformanceInfo.builder()
                .tableName(entry.getKey())
                .totalQueries(entry.getValue().getTotalQueries())
                .avgExecutionTime(entry.getValue().getAvgExecutionTime())
                .cacheHitRate(entry.getValue().getCacheHitRate())
                .build())
            .collect(Collectors.toList());
    }
    
    public List<SlowQueryInfo> getSlowQueries(Integer threshold, Integer limit) {
        double slowThreshold = threshold != null ? threshold : 1000.0; // 默认1秒
        
        List<QueryPerformanceStats> slowQueries = queryPerformanceRepository
            .findSlowQueries(slowThreshold);
        
        return slowQueries.stream()
            .limit(limit != null ? limit : 20)
            .map(this::convertToSlowQueryInfo)
            .collect(Collectors.toList());
    }
    
    public void recordQueryExecution(String queryHash, String queryText, String tableName, 
                                   long executionTime, boolean cacheHit) {
        QueryPerformanceStats stats = queryPerformanceRepository
            .findByQueryHash(queryHash)
            .orElse(new QueryPerformanceStats());
        
        if (stats.getId() == null) {
            stats.setQueryHash(queryHash);
            stats.setQueryText(queryText);
            stats.setTableName(tableName);
        }
        
        // 更新统计信息
        stats.setExecutionCount(stats.getExecutionCount() + 1);
        stats.setTotalExecutionTimeMs(stats.getTotalExecutionTimeMs() + executionTime);
        stats.setAvgExecutionTimeMs((double) stats.getTotalExecutionTimeMs() / stats.getExecutionCount());
        stats.setMaxExecutionTimeMs(Math.max(stats.getMaxExecutionTimeMs(), (int) executionTime));
        stats.setMinExecutionTimeMs(Math.min(stats.getMinExecutionTimeMs(), (int) executionTime));
        
        if (cacheHit) {
            stats.setCacheHitCount(stats.getCacheHitCount() + 1);
        } else {
            stats.setCacheMissCount(stats.getCacheMissCount() + 1);
        }
        
        stats.setLastExecutedAt(LocalDateTime.now());
        queryPerformanceRepository.save(stats);
    }
}
```

#### 2.3 系统配置服务

**2.3.1 SystemConfigurationService**
```java
@Service
@Transactional
public class SystemConfigurationService {
    
    @Autowired
    private SystemConfigRepository configRepository;
    
    @Autowired
    private ConfigChangeHistoryRepository changeHistoryRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public Map<String, Object> getSystemSettings() {
        List<SystemConfig> configs = configRepository.findByCategory("system");
        
        return configs.stream()
            .filter(config -> !config.isSensitive())
            .collect(Collectors.toMap(
                SystemConfig::getKeyName,
                this::convertConfigValue
            ));
    }
    
    public Map<String, Object> updateSystemSettings(Map<String, Object> settings, String updatedBy) {
        Map<String, Object> updatedConfigs = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 验证设置值
            if (!validateSystemSetting(key, value)) {
                throw new InvalidConfigurationException("无效的系统设置: " + key);
            }
            
            // 查找或创建配置项
            SystemConfig config = configRepository
                .findByCategoryAndKeyName("system", key)
                .orElse(new SystemConfig());
            
            if (config.getId() == null) {
                config.setId("system." + key);
                config.setCategory("system");
                config.setKeyName(key);
            }
            
            // 记录旧值用于历史记录
            String oldValue = config.getValue();
            
            // 更新配置值
            config.setValue(convertToString(value));
            config.setValueType(determineValueType(value));
            config.setUpdatedAt(LocalDateTime.now());
            
            configRepository.save(config);
            
            // 记录变更历史
            recordConfigChange(config.getId(), oldValue, config.getValue(), updatedBy, "系统设置更新");
            
            updatedConfigs.put(key, value);
        }
        
        // 发布配置变更事件
        eventPublisher.publishEvent(new SystemConfigChangedEvent(updatedConfigs));
        
        return updatedConfigs;
    }
    
    public Map<String, Object> getCacheSettings() {
        List<SystemConfig> configs = configRepository.findByCategory("cache");
        
        return configs.stream()
            .collect(Collectors.toMap(
                SystemConfig::getKeyName,
                this::convertConfigValue
            ));
    }
    
    public Map<String, Object> updateCacheSettings(Map<String, Object> settings, String updatedBy) {
        Map<String, Object> updatedConfigs = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 验证缓存设置
            if (!validateCacheSetting(key, value)) {
                throw new InvalidConfigurationException("无效的缓存设置: " + key);
            }
            
            SystemConfig config = configRepository
                .findByCategoryAndKeyName("cache", key)
                .orElse(new SystemConfig());
            
            if (config.getId() == null) {
                config.setId("cache." + key);
                config.setCategory("cache");
                config.setKeyName(key);
            }
            
            String oldValue = config.getValue();
            config.setValue(convertToString(value));
            config.setValueType(determineValueType(value));
            config.setUpdatedAt(LocalDateTime.now());
            
            configRepository.save(config);
            
            recordConfigChange(config.getId(), oldValue, config.getValue(), updatedBy, "缓存设置更新");
            updatedConfigs.put(key, value);
        }
        
        // 发布缓存配置变更事件
        eventPublisher.publishEvent(new CacheConfigChangedEvent(updatedConfigs));
        
        return updatedConfigs;
    }
    
    private boolean validateSystemSetting(String key, Object value) {
        // 实现真实的系统设置验证逻辑
        switch (key) {
            case "maxConnections":
                return value instanceof Integer && (Integer) value > 0 && (Integer) value <= 1000;
            case "connectionTimeout":
                return value instanceof Integer && (Integer) value >= 1000 && (Integer) value <= 60000;
            case "enableLogging":
                return value instanceof Boolean;
            default:
                return true; // 允许未知设置
        }
    }
    
    private boolean validateCacheSetting(String key, Object value) {
        // 实现真实的缓存设置验证逻辑
        switch (key) {
            case "defaultTtl":
                return value instanceof String && isValidDuration((String) value);
            case "maxSize":
                return value instanceof Integer && (Integer) value > 0;
            case "evictionPolicy":
                return value instanceof String && Arrays.asList("LRU", "LFU", "FIFO").contains((String) value);
            default:
                return true;
        }
    }
}
```

### 阶段三：控制器层重构

#### 3.1 CacheStatsController 重构

**3.1.1 完整的实现**
```java
@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
public class CacheStatsController {
    
    @Autowired
    private CacheStatisticsService cacheStatisticsService;
    
    @Autowired
    private CacheRuleService cacheRuleService;
    
    /**
     * 3.1 缓存概览统计
     * GET /api/cache/stats/overview
     */
    @GetMapping("/stats/overview")
    public ApiResponse<CacheOverviewStats> getCacheOverview() {
        try {
            log.debug("Getting cache overview statistics");
            CacheOverviewStats stats = cacheStatisticsService.getCacheOverview();
            return ApiResponse.success(stats, "获取缓存概览统计成功");
        } catch (Exception e) {
            log.error("Failed to get cache overview statistics", e);
            return ApiResponse.error("CACHE_OVERVIEW_ERROR", "获取缓存概览统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.2 缓存命中率统计
     * GET /api/cache/stats/hit-rate
     */
    @GetMapping("/stats/hit-rate")
    public ApiResponse<List<CacheHitRateStats>> getCacheHitRateStats(
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            log.debug("Getting cache hit rate statistics: ruleId={}, tableName={}, hours={}", 
                     ruleId, tableName, hours);
            
            List<CacheHitRateStats> stats = cacheStatisticsService.getCacheHitRateStats(ruleId, tableName, hours);
            return ApiResponse.success(stats, "获取缓存命中率统计成功");
        } catch (Exception e) {
            log.error("Failed to get cache hit rate statistics", e);
            return ApiResponse.error("CACHE_HIT_RATE_ERROR", "获取缓存命中率统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.3 查询性能统计
     * GET /api/cache/stats/queries
     */
    @GetMapping("/stats/queries")
    public ApiResponse<List<QueryPerformanceInfo>> getQueryPerformanceStats(
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "executionCount") String sortBy) {
        try {
            log.debug("Getting query performance statistics: tableName={}, limit={}, sortBy={}", 
                     tableName, limit, sortBy);
            
            List<QueryPerformanceInfo> stats = cacheStatisticsService.getQueryPerformanceStats(tableName, limit, sortBy);
            return ApiResponse.success(stats, "获取查询性能统计成功");
        } catch (Exception e) {
            log.error("Failed to get query performance statistics", e);
            return ApiResponse.error("QUERY_PERFORMANCE_ERROR", "获取查询性能统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.4 热门表格统计
     * GET /api/cache/stats/tables
     */
    @GetMapping("/stats/tables")
    public ApiResponse<List<TablePerformanceInfo>> getTablePerformanceStats() {
        try {
            log.debug("Getting table performance statistics");
            List<TablePerformanceInfo> stats = cacheStatisticsService.getTablePerformanceStats();
            return ApiResponse.success(stats, "获取热门表格统计成功");
        } catch (Exception e) {
            log.error("Failed to get table performance statistics", e);
            return ApiResponse.error("TABLE_PERFORMANCE_ERROR", "获取热门表格统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.5 慢查询统计
     * GET /api/cache/stats/slow-queries
     */
    @GetMapping("/stats/slow-queries")
    public ApiResponse<List<SlowQueryInfo>> getSlowQueries(
            @RequestParam(defaultValue = "1000") int threshold,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            log.debug("Getting slow queries: threshold={}, limit={}", threshold, limit);
            List<SlowQueryInfo> slowQueries = cacheStatisticsService.getSlowQueries(threshold, limit);
            return ApiResponse.success(slowQueries, "获取慢查询统计成功");
        } catch (Exception e) {
            log.error("Failed to get slow queries", e);
            return ApiResponse.error("SLOW_QUERIES_ERROR", "获取慢查询统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.6 获取查询列表信息
     * GET /api/cache/queries
     */
    @GetMapping("/queries")
    public ApiResponse<List<QueryInfo>> getQueries(
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.debug("Getting queries: tableName={}, limit={}", tableName, limit);
            List<QueryInfo> queries = cacheStatisticsService.getQueries(tableName, limit);
            return ApiResponse.success(queries, "获取查询列表成功");
        } catch (Exception e) {
            log.error("Failed to get queries", e);
            return ApiResponse.error("QUERIES_ERROR", "获取查询列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.7 获取特定查询的缓存规则
     * GET /api/cache/queries/{queryHash}/rules
     */
    @GetMapping("/queries/{queryHash}/rules")
    public ApiResponse<List<CacheRuleInfo>> getQueryCacheRules(@PathVariable String queryHash) {
        try {
            log.debug("Getting cache rules for query: {}", queryHash);
            List<CacheRuleInfo> rules = cacheRuleService.getCacheRulesForQuery(queryHash);
            return ApiResponse.success(rules, "获取查询缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get cache rules for query: {}", queryHash, e);
            return ApiResponse.error("QUERY_RULES_ERROR", "获取查询缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.8 为特定查询创建缓存规则
     * POST /api/cache/queries/{queryHash}/rules
     */
    @PostMapping("/queries/{queryHash}/rules")
    public ApiResponse<CacheRuleInfo> createQueryCacheRule(
            @PathVariable String queryHash,
            @RequestBody CreateCacheRuleRequest request) {
        try {
            log.debug("Creating cache rule for query: {}, request: {}", queryHash, request);
            CacheRuleInfo rule = cacheRuleService.createCacheRuleForQuery(queryHash, request);
            return ApiResponse.success(rule, "为查询创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create cache rule for query: {}", queryHash, e);
            return ApiResponse.error("CREATE_QUERY_RULE_ERROR", "为查询创建缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.9 获取表格列表信息
     * GET /api/cache/tables
     */
    @GetMapping("/tables")
    public ApiResponse<List<TableInfo>> getTables(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.debug("Getting tables: limit={}", limit);
            List<TableInfo> tables = cacheStatisticsService.getTables(limit);
            return ApiResponse.success(tables, "获取表格列表成功");
        } catch (Exception e) {
            log.error("Failed to get tables", e);
            return ApiResponse.error("TABLES_ERROR", "获取表格列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.10 获取特定表格的缓存规则
     * GET /api/cache/tables/{tableName}/rules
     */
    @GetMapping("/tables/{tableName}/rules")
    public ApiResponse<List<CacheRuleInfo>> getTableCacheRules(@PathVariable String tableName) {
        try {
            log.debug("Getting cache rules for table: {}", tableName);
            List<CacheRuleInfo> rules = cacheRuleService.getCacheRulesForTable(tableName);
            return ApiResponse.success(rules, "获取表格缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get cache rules for table: {}", tableName, e);
            return ApiResponse.error("TABLE_RULES_ERROR", "获取表格缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.11 为特定表格创建缓存规则
     * POST /api/cache/tables/{tableName}/rules
     */
    @PostMapping("/tables/{tableName}/rules")
    public ApiResponse<CacheRuleInfo> createTableCacheRule(
            @PathVariable String tableName,
            @RequestBody CreateCacheRuleRequest request) {
        try {
            log.debug("Creating cache rule for table: {}, request: {}", tableName, request);
            CacheRuleInfo rule = cacheRuleService.createCacheRuleForTable(tableName, request);
            return ApiResponse.success(rule, "为表格创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create cache rule for table: {}", tableName, e);
            return ApiResponse.error("CREATE_TABLE_RULE_ERROR", "为表格创建缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 3.12 获取特定表格的统计信息
     * GET /api/cache/tables/{tableName}/stats
     */
    @GetMapping("/tables/{tableName}/stats")
    public ApiResponse<TableStatsInfo> getTableStats(@PathVariable String tableName) {
        try {
            log.debug("Getting stats for table: {}", tableName);
            TableStatsInfo stats = cacheStatisticsService.getTableStats(tableName);
            return ApiResponse.success(stats, "获取表格统计信息成功");
        } catch (Exception e) {
            log.error("Failed to get stats for table: {}", tableName, e);
            return ApiResponse.error("TABLE_STATS_ERROR", "获取表格统计信息失败: " + e.getMessage());
        }
    }
}
```

### 阶段四：数据模型定义

#### 4.1 实体类定义

**4.1.1 Server 实体**
```java
@Entity
@Table(name = "servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Server {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(name = "database_type")
    private String databaseType;
    
    private String username;
    
    @Column(name = "password_encrypted")
    private String passwordEncrypted;
    
    private String status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
    
    @Column(name = "connection_pool_size")
    private Integer connectionPoolSize;
    
    @Column(name = "max_connections")
    private Integer maxConnections;
    
    @Column(name = "timeout_ms")
    private Integer timeoutMs;
    
    @Column(name = "is_active")
    private Boolean isActive;
}
```

**4.1.2 CacheRule 实体**
```java
@Entity
@Table(name = "cache_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheRule {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;
    
    @Column(nullable = false)
    private String pattern;
    
    @Column(name = "ttl_seconds", nullable = false)
    private Integer ttlSeconds;
    
    private Integer priority;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    private String description;
}
```

**4.1.3 SystemConfig 实体**
```java
@Entity
@Table(name = "system_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "key_name", nullable = false)
    private String keyName;
    
    private String value;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type")
    private ConfigValueType valueType;
    
    private String description;
    
    @Column(name = "is_sensitive")
    private Boolean isSensitive;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**4.1.4 QueryPerformanceStats 实体**
```java
@Entity
@Table(name = "query_performance_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPerformanceStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "query_hash", nullable = false)
    private String queryHash;
    
    @Column(name = "query_text", columnDefinition = "TEXT")
    private String queryText;
    
    @Column(name = "table_name")
    private String tableName;
    
    @Column(name = "execution_count")
    private Long executionCount;
    
    @Column(name = "total_execution_time_ms")
    private Long totalExecutionTimeMs;
    
    @Column(name = "avg_execution_time_ms")
    private Double avgExecutionTimeMs;
    
    @Column(name = "max_execution_time_ms")
    private Integer maxExecutionTimeMs;
    
    @Column(name = "min_execution_time_ms")
    private Integer minExecutionTimeMs;
    
    @Column(name = "cache_hit_count")
    private Long cacheHitCount;
    
    @Column(name = "cache_miss_count")
    private Long cacheMissCount;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 阶段五：配置和初始化

#### 5.1 数据库配置

**5.1.1 application.yml 数据库配置**
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/ojp-server
    username: sa
    password: 
    driver-class-name: org.h2.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
    defer-datasource-initialization: true
  
  h2:
    console:
      enabled: true
      path: /h2-console
```

**5.1.2 数据库初始化脚本**
```sql
-- 初始化系统配置
INSERT INTO system_configs (id, category, key_name, value, value_type, description, is_sensitive) VALUES
('system.maxConnections', 'system', 'maxConnections', '100', 'INTEGER', '最大数据库连接数', false),
('system.connectionTimeout', 'system', 'connectionTimeout', '30000', 'INTEGER', '连接超时时间(毫秒)', false),
('system.enableLogging', 'system', 'enableLogging', 'true', 'BOOLEAN', '启用日志记录', false),
('cache.defaultTtl', 'cache', 'defaultTtl', '30m', 'STRING', '默认缓存TTL', false),
('cache.maxSize', 'cache', 'maxSize', '1000', 'INTEGER', '最大缓存条目数', false),
('cache.evictionPolicy', 'cache', 'evictionPolicy', 'LRU', 'STRING', '缓存淘汰策略', false);

-- 初始化默认缓存规则
INSERT INTO cache_rules (id, name, rule_type, pattern, ttl_seconds, priority, is_active, created_at, updated_at, created_by, description) VALUES
('rule_default_tables', '默认表格缓存', 'TABLES', '.*', 1800, 0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', '默认的表格缓存规则'),
('rule_slow_queries', '慢查询缓存', 'QUERY', '.*', 300, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', '慢查询缓存规则');
```

#### 5.2 服务配置

**5.2.1 缓存引擎配置**
```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        List<Cache> caches = Arrays.asList(
            new ConcurrentMapCache("queryCache"),
            new ConcurrentMapCache("tableCache"),
            new ConcurrentMapCache("ruleCache")
        );
        
        cacheManager.setCaches(caches);
        return cacheManager;
    }
    
    @Bean
    public SmartCacheEngine smartCacheEngine() {
        return new SmartCacheEngine();
    }
}
```

**5.2.2 事件配置**
```java
@Configuration
@EnableAsync
public class EventConfiguration {
    
    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
}
```

### 阶段六：测试和验证

#### 6.1 单元测试

**6.1.1 CacheStatisticsServiceTest**
```java
@ExtendWith(MockitoExtension.class)
class CacheStatisticsServiceTest {
    
    @Mock
    private QueryPerformanceRepository queryPerformanceRepository;
    
    @Mock
    private CacheHitStatsRepository cacheHitStatsRepository;
    
    @Mock
    private CacheRuleRepository cacheRuleRepository;
    
    @Mock
    private SmartCacheEngine cacheEngine;
    
    @InjectMocks
    private CacheStatisticsService cacheStatisticsService;
    
    @Test
    void getCacheOverview_ShouldReturnValidStats() {
        // Given
        when(cacheRuleRepository.countByIsActiveTrue()).thenReturn(5L);
        when(cacheEngine.getCacheSize()).thenReturn(100L);
        
        // When
        CacheOverviewStats stats = cacheStatisticsService.getCacheOverview();
        
        // Then
        assertNotNull(stats);
        assertEquals(5L, stats.getActiveRules());
        assertEquals(100L, stats.getCacheSize());
    }
    
    @Test
    void getQueryPerformanceStats_ShouldReturnValidData() {
        // Given
        List<QueryPerformanceStats> mockStats = createMockQueryStats();
        when(queryPerformanceRepository.findAll(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(mockStats));
        
        // When
        List<QueryPerformanceInfo> result = cacheStatisticsService.getQueryPerformanceStats(null, 10, "executionCount");
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
```

#### 6.2 集成测试

**6.2.1 CacheStatsControllerIntegrationTest**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CacheStatsControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private CacheStatisticsService cacheStatisticsService;
    
    @Test
    void getCacheOverview_ShouldReturnSuccess() {
        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/cache/stats/overview", ApiResponse.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
    }
    
    @Test
    void getQueryPerformanceStats_ShouldReturnValidData() {
        // Given
        String url = "/api/cache/stats/queries?limit=10";
        
        // When
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(url, ApiResponse.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getStatus());
    }
}
```

## 实施计划

### 阶段一：数据层实现 (1-2周)
1. 创建数据库表结构
2. 实现实体类和Repository
3. 配置数据库连接
4. 编写数据访问层测试

### 阶段二：服务层实现 (2-3周)
1. 实现ServerManagementService
2. 实现CacheStatisticsService
3. 实现SystemConfigurationService
4. 编写服务层单元测试

### 阶段三：控制器层重构 (1周)
1. 重构CacheStatsController
2. 重构ServerController
3. 重构SystemSettingsController
4. 编写控制器集成测试

### 阶段四：配置和优化 (1周)
1. 完善配置文件
2. 实现缓存引擎集成
3. 添加事件处理机制
4. 性能优化和调优

### 阶段五：测试和部署 (1周)
1. 端到端测试
2. 性能测试
3. 文档更新
4. 生产环境部署

## 风险评估

### 技术风险
1. **数据库迁移风险**: 现有数据需要迁移到新结构
2. **性能影响**: 数据库查询可能影响性能
3. **兼容性风险**: 新API可能与现有客户端不兼容

### 缓解措施
1. **数据迁移策略**: 制定详细的数据迁移计划
2. **性能监控**: 实施全面的性能监控
3. **向后兼容**: 保持API向后兼容性
4. **渐进式部署**: 采用蓝绿部署策略

## 预期收益

### 功能改进
1. **真实数据**: 所有统计和配置基于真实数据
2. **持久化**: 数据持久化存储，重启后数据不丢失
3. **可扩展性**: 支持大规模数据和高并发访问
4. **可维护性**: 代码结构清晰，易于维护和扩展

### 性能提升
1. **查询优化**: 数据库查询经过优化
2. **缓存效率**: 智能缓存策略提升性能
3. **响应时间**: 减少API响应时间
4. **资源利用**: 更好的资源利用率

### 运维改进
1. **监控能力**: 完整的系统监控
2. **故障诊断**: 详细的日志和错误信息
3. **配置管理**: 灵活的配置管理
4. **备份恢复**: 数据备份和恢复机制

## 结论

本设计方案将彻底解决OJP Server中的模拟实现问题，将其转换为生产级的真实实现。通过分阶段实施，可以确保系统的稳定性和可靠性，同时提供更好的性能和用户体验。

建议按照本方案逐步实施，每个阶段完成后进行充分的测试和验证，确保系统质量。
