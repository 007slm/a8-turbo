# CacheDecisionService 实现文档

## 1. 实现概述

### 1.1 核心功能
`CacheDecisionService` 基于表级 CDC 同步状态进行缓存决策，决定查询是否路由到 StarRocks 缓存还是回源查询 MySQL。

### 1.2 设计原则
- **状态驱动**: 基于表同步状态的决策
- **简单高效**: O(1) 决策复杂度
- **易于集成**: 无复杂配置，开箱即用

## 2. 核心实现

### 2.1 决策逻辑

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheDecisionService {

    private final TableSyncStateManager tableSyncStateManager;
    private final CacheMetrics cacheMetrics;

    /**
     * 判断给定 SQL 是否应该路由到 StarRocks 缓存
     */
    public boolean makeDecision(String connHash, String sql) {
        long startTime = System.currentTimeMillis();

        // 解析 SQL 中涉及的表名
        Set<String> involvedTables = JSqlParserUtil.extractTableNames(sql);

        if (involvedTables.isEmpty()) {
            log.debug("无法从 SQL 提取表名，回源查询");
            recordDecision(startTime, false);
            return false;
        }

        // 检查每个表的同步状态
        for (String table : involvedTables) {
            if (!tableSyncStateManager.isTableReady(connHash, table)) {
                long decisionTime = System.currentTimeMillis() - startTime;
                log.info("缓存决策: 表 {} 未就绪, 回源查询. decisionTime={}ms", table, decisionTime);
                recordDecision(startTime, false);
                return false;
            }
        }

        long decisionTime = System.currentTimeMillis() - startTime;
        log.info("缓存决策: 所有表就绪 ({}), 走 StarRocks. decisionTime={}ms", involvedTables, decisionTime);
        recordDecision(startTime, true);
        return true;
    }

    private void recordDecision(long startTime, boolean hit) {
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.recordDecisionLatency(duration);
        if (hit) {
            cacheMetrics.recordCacheHit();
        } else {
            cacheMetrics.recordCacheMiss();
        }
    }
}
```

### 2.2 表同步状态管理

```java
@Service
@Slf4j
public class TableSyncStateManager {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查表是否已同步就绪
     */
    public boolean isTableReady(String connHash, String tableName) {
        String key = buildSyncStateKey(connHash, tableName);
        String state = (String) redisTemplate.opsForValue().get(key);
        return "READY".equals(state);
    }

    private String buildSyncStateKey(String connHash, String tableName) {
        return String.format("ojp:cdc:sync:state:%s:%s", connHash, tableName);
    }
}
```

### 2.3 SQL 表名解析

```java
@Component
@Slf4j
public class JSqlParserUtil {

    /**
     * 从 SQL 中提取表名
     */
    public static Set<String> extractTableNames(String sql) {
        Set<String> tables = new HashSet<>();

        try {
            CCJSqlParserManager parserManager = new CCJSqlParserManager();
            Statement statement = parserManager.parse(new StringReader(sql));

            if (statement instanceof Select) {
                Select select = (Select) statement;
                TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
                tables = new HashSet<>(tablesNamesFinder.getTableList(select));
            }

        } catch (Exception e) {
            log.warn("SQL 解析失败: {}", sql, e);
        }

        return tables.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }
}
```

## 3. 性能指标

### 3.1 决策延迟监控

```java
@Component
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    public void recordDecisionLatency(long durationMs) {
        Timer.builder("cache.decision.latency")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordCacheHit() {
        Counter.builder("cache.decision.hit")
            .register(meterRegistry)
            .increment();
    }

    public void recordCacheMiss() {
        Counter.builder("cache.decision.miss")
            .register(meterRegistry)
            .increment();
    }
}
```

## 4. 集成方式

### 4.1 JDBC 驱动集成

```java
public class SmartConnection implements Connection {

    private final CacheDecisionService cacheDecisionService;

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        boolean useCache = cacheDecisionService.makeDecision(connHash, sql);

        if (useCache) {
            // 路由到 StarRocks
            return executeOnStarRocks(sql);
        } else {
            // 回源到 MySQL
            return executeOnMySQL(sql);
        }
    }
}
```

## 5. 配置要求

### 5.1 Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 5.2 CDC 状态键格式

- 同步状态键: `ojp:cdc:sync:state:{connHash}:{tableName}`
- 状态值: `READY` (就绪) 或 `SYNCING` (同步中)

## 6. 错误处理

### 6.1 降级策略

- SQL 解析失败: 回源查询
- Redis 连接异常: 回源查询
- 表状态未知: 回源查询

### 6.2 日志记录

```java
// 决策日志示例
log.info("缓存决策: 表 {} 未就绪, 回源查询. decisionTime={}ms", table, decisionTime);
log.info("缓存决策: 所有表就绪 ({}), 走 StarRocks. decisionTime={}ms", involvedTables, decisionTime);
```

---

**文档版本**: v2.0  
**更新时间**: 2024-01-24  
**实现状态**: 已完成  
**核心特点**: 基于状态的决策，简单高效