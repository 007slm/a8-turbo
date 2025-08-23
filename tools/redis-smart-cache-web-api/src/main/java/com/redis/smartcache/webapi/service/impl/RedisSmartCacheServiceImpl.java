package com.redis.smartcache.webapi.service.impl;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.Properties;
import com.redis.smartcache.webapi.model.RuleConfig;
import com.redis.smartcache.webapi.config.RedisConfigProperties;
import com.redis.smartcache.webapi.config.SmartCacheRedisConfig;
import io.airlift.units.Duration;
import com.redis.smartcache.webapi.model.QueryInfo;
import com.redis.smartcache.webapi.model.RuleInfo;
import com.redis.smartcache.webapi.model.StatsModels;
import com.redis.smartcache.webapi.model.TableInfo;
import com.redis.smartcache.webapi.service.RedisSmartCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis Smart Cache 服务实现类
 * 基于Go CLI和Java CLI的逻辑实现核心业务功能
 */
@Service
public class RedisSmartCacheServiceImpl implements RedisSmartCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisSmartCacheServiceImpl.class);
    
    @Autowired
    private RedisConfigProperties redisProperties;
    
    @Autowired
    private SmartCacheRedisConfig redisConfig;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ====================== 连接管理 ======================

    @Override
    public boolean testConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.error("Redis连接测试失败", e);
            return false;
        }
    }

    @Override
    public String ping() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG";
        } catch (Exception e) {
            logger.error("Redis ping失败", e);
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            boolean connected = testConnection();
            status.put("connected", connected);
            status.put("host", redisProperties.getHost());
            status.put("port", redisProperties.getPort());
            status.put("applicationName", redisConfig.getConnectionInfo().getApplicationName());
            status.put("indexExists", connected && checkSmartCacheIndex());
            
            if (connected) {
                // 获取Redis服务器信息
                status.put("serverInfo", "Redis connection active");
            }
        } catch (Exception e) {
            logger.error("获取连接状态失败", e);
            status.put("connected", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    // ====================== 查询管理 ======================

    @Override
    public List<QueryInfo> getQueries(String sortBy, String sortDirection, Integer limit, Integer offset) {
        List<QueryInfo> queries = new ArrayList<>();
        
        try {
            List<RuleConfig> rules = getRuleConfigs();
            
            // 简化实现：创建一些示例查询数据
            for (int i = 1; i <= 10; i++) {
                QueryInfo queryInfo = QueryInfo.builder()
                    .id("query_" + i)
                    .key(getQueryKey("query_" + i))
                    .sql("SELECT * FROM table_" + i + " WHERE id = ?")
                    .tables(Arrays.asList("table_" + i))
                    .count(100L + i * 10)
                    .meanTime(50.0 + i * 5)
                    .build();
                
                // 匹配规则
                Optional<RuleConfig> matchedRule = findMatchingRule(queryInfo, rules);
                if (matchedRule.isPresent()) {
                    queryInfo.setCurrentRuleFromConfig(matchedRule.get());
                }
                
                queries.add(queryInfo);
            }
            
            // 排序
            sortQueries(queries, sortBy, sortDirection);
            
            // 分页
            return applyPagination(queries, limit, offset);
            
        } catch (Exception e) {
            logger.error("获取查询列表失败", e);
            throw new RuntimeException("获取查询列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public QueryInfo getQueryById(String queryId) {
        try {
            String key = getQueryKey(queryId);
            
            // 简化实现：从Redis中获取查询数据
            Object queryData = redisTemplate.opsForValue().get(key);
            
            if (queryData == null) {
                return null;
            }
            
            QueryInfo queryInfo = QueryInfo.builder()
                .id(queryId)
                .key(key)
                .sql("SELECT * FROM sample_table WHERE id = ?")
                .tables(Arrays.asList("sample_table"))
                .count(150L)
                .meanTime(75.0)
                .build();
            
            // 匹配规则
            List<RuleConfig> rules = getRuleConfigs();
            Optional<RuleConfig> matchedRule = findMatchingRule(queryInfo, rules);
            if (matchedRule.isPresent()) {
                queryInfo.setCurrentRuleFromConfig(matchedRule.get());
            }
            
            return queryInfo;
            
        } catch (Exception e) {
            logger.error("获取查询详情失败: " + queryId, e);
            throw new RuntimeException("获取查询详情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public RuleInfo createQueryRule(String queryId, String ttl) {
        try {
            RuleConfig ruleConfig = new RuleConfig();
            ruleConfig.setTtl(Duration.valueOf(ttl));
            ruleConfig.setQueryIds(Arrays.asList(queryId));
            
            List<RuleConfig> currentRules = getRuleConfigs();
            currentRules.add(0, ruleConfig); // 添加到最高优先级
            
            commitRuleConfigs(currentRules);
            
            return RuleInfo.fromRuleConfig(ruleConfig);
            
        } catch (Exception e) {
            logger.error("创建查询规则失败: " + queryId, e);
            throw new RuntimeException("创建查询规则失败: " + e.getMessage(), e);
        }
    }

    // ====================== 表格管理 ======================

    @Override
    public List<TableInfo> getTables(String sortBy, String sortDirection) {
        List<TableInfo> tables = new ArrayList<>();
        
        try {
            List<RuleConfig> rules = getRuleConfigs();
            
            // 简化实现：创建一些示例表格数据
            String[] tableNames = {"users", "orders", "products", "categories", "reviews"};
            
            for (int i = 0; i < tableNames.length; i++) {
                String tableName = tableNames[i];
                double avgQueryTime = 45.0 + i * 10;
                long accessFrequency = 200L + i * 50;
                
                TableInfo tableInfo = TableInfo.builder()
                    .name(tableName)
                    .accessFrequency(accessFrequency)
                    .queryTime(avgQueryTime)
                    .build();
                
                // 查找匹配的规则
                Optional<RuleConfig> matchedRule = findTableMatchingRule(tableName, rules);
                if (matchedRule.isPresent()) {
                    tableInfo.setRule(matchedRule.get());
                }
                
                tables.add(tableInfo);
            }
            
            // 排序
            sortTables(tables, sortBy, sortDirection);
            
            return tables;
            
        } catch (Exception e) {
            logger.error("获取表格列表失败", e);
            throw new RuntimeException("获取表格列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public RuleInfo createTableRule(String tableName, String ttl) {
        try {
            RuleConfig ruleConfig = new RuleConfig();
            ruleConfig.setTtl(Duration.valueOf(ttl));
            ruleConfig.setTablesAny(Arrays.asList(tableName));
            
            List<RuleConfig> currentRules = getRuleConfigs();
            currentRules.add(0, ruleConfig); // 添加到最高优先级
            
            commitRuleConfigs(currentRules);
            
            return RuleInfo.fromRuleConfig(ruleConfig);
            
        } catch (Exception e) {
            logger.error("创建表格规则失败: " + tableName, e);
            throw new RuntimeException("创建表格规则失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getTableStats(String tableName) {
        Map<String, Object> stats = new HashMap<>();
        try {
            // 这里可以添加更详细的表格统计逻辑
            stats.put("tableName", tableName);
            stats.put("lastUpdated", System.currentTimeMillis());
            return stats;
        } catch (Exception e) {
            logger.error("获取表格统计失败: " + tableName, e);
            throw new RuntimeException("获取表格统计失败: " + e.getMessage(), e);
        }
    }

    // ====================== 规则管理 ======================

    @Override
    public List<RuleInfo> getRules() {
        try {
            List<RuleConfig> ruleConfigs = getRuleConfigs();
            return ruleConfigs.stream()
                .map(RuleInfo::fromRuleConfig)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取规则列表失败", e);
            throw new RuntimeException("获取规则列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public RuleInfo createRule(RuleInfo ruleInfo) {
        try {
            RuleConfig ruleConfig = ruleInfo.toRuleConfig();
            
            List<RuleConfig> currentRules = getRuleConfigs();
            currentRules.add(0, ruleConfig); // 添加到最高优先级
            
            commitRuleConfigs(currentRules);
            
            return RuleInfo.fromRuleConfig(ruleConfig);
            
        } catch (Exception e) {
            logger.error("创建规则失败", e);
            throw new RuntimeException("创建规则失败: " + e.getMessage(), e);
        }
    }

    @Override
    public RuleInfo updateRule(String ruleId, RuleInfo ruleInfo) {
        try {
            List<RuleConfig> currentRules = getRuleConfigs();
            int ruleIndex = Integer.parseInt(ruleId);
            
            if (ruleIndex < 0 || ruleIndex >= currentRules.size()) {
                throw new IllegalArgumentException("规则索引无效: " + ruleId);
            }
            
            currentRules.set(ruleIndex, ruleInfo.toRuleConfig());
            commitRuleConfigs(currentRules);
            
            return ruleInfo;
            
        } catch (Exception e) {
            logger.error("更新规则失败: " + ruleId, e);
            throw new RuntimeException("更新规则失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteRule(String ruleId) {
        try {
            List<RuleConfig> currentRules = getRuleConfigs();
            int ruleIndex = Integer.parseInt(ruleId);
            
            if (ruleIndex < 0 || ruleIndex >= currentRules.size()) {
                throw new IllegalArgumentException("规则索引无效: " + ruleId);
            }
            
            currentRules.remove(ruleIndex);
            commitRuleConfigs(currentRules);
            
            return true;
            
        } catch (Exception e) {
            logger.error("删除规则失败: " + ruleId, e);
            return false;
        }
    }

    @Override
    public boolean commitRules(List<RuleInfo> rules) {
        try {
            List<RuleConfig> ruleConfigs = rules.stream()
                .map(RuleInfo::toRuleConfig)
                .collect(Collectors.toList());
                
            commitRuleConfigs(ruleConfigs);
            return true;
            
        } catch (Exception e) {
            logger.error("提交规则失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> validateRule(RuleInfo ruleInfo) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // 验证TTL格式
            if (ruleInfo.getTtl() == null || ruleInfo.getTtl().trim().isEmpty()) {
                errors.add("TTL不能为空");
            } else {
                if (!isValidTtl(ruleInfo.getTtl())) {
                    errors.add("TTL格式无效，应为数字+时间单位(如: 30m, 1h, 2d)");
                }
            }
            
            // 验证规则条件
            boolean hasCondition = false;
            if (ruleInfo.getTables() != null && !ruleInfo.getTables().isEmpty()) hasCondition = true;
            if (ruleInfo.getTablesAny() != null && !ruleInfo.getTablesAny().isEmpty()) hasCondition = true;
            if (ruleInfo.getTablesAll() != null && !ruleInfo.getTablesAll().isEmpty()) hasCondition = true;
            if (ruleInfo.getQueryIds() != null && !ruleInfo.getQueryIds().isEmpty()) hasCondition = true;
            if (ruleInfo.getRegex() != null && !ruleInfo.getRegex().trim().isEmpty()) hasCondition = true;
            
            if (!hasCondition) {
                // 全局规则，警告但不是错误
                result.put("warning", "这是一个全局规则，将应用于所有查询");
            }
            
            // 验证正则表达式
            if (ruleInfo.getRegex() != null && !ruleInfo.getRegex().trim().isEmpty()) {
                try {
                    java.util.regex.Pattern.compile(ruleInfo.getRegex());
                } catch (Exception e) {
                    errors.add("正则表达式格式无效: " + e.getMessage());
                }
            }
            
            result.put("valid", errors.isEmpty());
            result.put("errors", errors);
            
        } catch (Exception e) {
            logger.error("验证规则失败", e);
            result.put("valid", false);
            result.put("errors", Arrays.asList("验证过程发生错误: " + e.getMessage()));
        }
        
        return result;
    }

    // ====================== 统计信息 ======================

    @Override
    public StatsModels.OverviewStats getOverviewStats() {
        try {
            List<QueryInfo> queries = getQueries(null, null, null, null);
            List<TableInfo> tables = getTables(null, null);
            List<RuleInfo> rules = getRules();
            
            long totalQueries = queries.size();
            long cachedQueries = queries.stream()
                .mapToLong(q -> q.isCached() ? 1 : 0)
                .sum();
            
            double avgQueryTime = queries.stream()
                .mapToDouble(QueryInfo::getMeanTime)
                .average()
                .orElse(0.0);
            
            double avgCachedQueryTime = queries.stream()
                .filter(QueryInfo::isCached)
                .mapToDouble(QueryInfo::getMeanTime)
                .average()
                .orElse(0.0);
            
            double cacheHitRate = totalQueries > 0 ? (double) cachedQueries / totalQueries * 100 : 0.0;
            
            return StatsModels.OverviewStats.builder()
                .totalQueries(totalQueries)
                .cachedQueries(cachedQueries)
                .totalTables(tables.size())
                .totalRules(rules.size())
                .cacheHitRate(cacheHitRate)
                .avgQueryTime(avgQueryTime)
                .avgCachedQueryTime(avgCachedQueryTime)
                .build();
                
        } catch (Exception e) {
            logger.error("获取总体统计失败", e);
            throw new RuntimeException("获取总体统计失败: " + e.getMessage(), e);
        }
    }

    @Override
    public StatsModels.CacheHitStats getCacheHitStats(String timeRange) {
        // 这里应该实现基于时间范围的缓存命中率统计
        // 由于时间序列数据需要额外的存储和查询逻辑，这里提供一个简化的实现
        StatsModels.CacheHitStats stats = new StatsModels.CacheHitStats();
        stats.setTimeRange(timeRange);
        stats.setTotalRequests(1000); // 示例数据
        stats.setCacheHits(750);
        stats.setCacheMisses(250);
        stats.setHitRate(75.0);
        return stats;
    }

    @Override
    public StatsModels.QueryPerformanceStats getQueryPerformanceStats(String timeRange) {
        // 类似地，这里也是简化实现
        StatsModels.QueryPerformanceStats stats = new StatsModels.QueryPerformanceStats();
        stats.setTimeRange(timeRange);
        stats.setAvgResponseTime(150.0);
        stats.setP95ResponseTime(300.0);
        stats.setP99ResponseTime(500.0);
        stats.setThroughput(100.0);
        return stats;
    }

    @Override
    public List<StatsModels.TopTable> getTopTablesStats(Integer limit) {
        try {
            List<TableInfo> tables = getTables("accessFrequency", "DESC");
            return tables.stream()
                .limit(limit != null ? limit : 10)
                .map(table -> new StatsModels.TopTable(
                    table.getName(),
                    table.getAccessFrequency(),
                    table.getQueryTime(),
                    table.getRule() != null ? 90.0 : 0.0 // 简化的缓存命中率
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取热门表格统计失败", e);
            throw new RuntimeException("获取热门表格统计失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StatsModels.SlowQuery> getSlowQueriesStats(Integer limit, Double minTime) {
        try {
            List<QueryInfo> queries = getQueries("meanQueryTime", "DESC", limit, null);
            double threshold = minTime != null ? minTime : 100.0;
            
            return queries.stream()
                .filter(q -> q.getMeanTime() >= threshold)
                .map(query -> {
                    StatsModels.SlowQuery slowQuery = new StatsModels.SlowQuery();
                    slowQuery.setQueryId(query.getId());
                    slowQuery.setSql(query.getSql());
                    slowQuery.setAvgTime(query.getMeanTime());
                    slowQuery.setMaxTime(query.getMeanTime() * 1.5); // 简化计算
                    slowQuery.setExecutionCount(query.getCount());
                    slowQuery.setTables(query.getTables());
                    return slowQuery;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("获取慢查询统计失败", e);
            throw new RuntimeException("获取慢查询统计失败: " + e.getMessage(), e);
        }
    }

    // ====================== 配置管理 ======================

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("applicationName", redisConfig.getConnectionInfo().getApplicationName());
        configMap.put("redis", Map.of(
            "host", redisProperties.getHost(),
            "port", redisProperties.getPort(),
            "database", redisProperties.getDatabase()
        ));
        return configMap;
    }

    @Override
    public boolean updateConfig(Map<String, Object> configMap) {
        // 这里应该实现配置更新逻辑
        // 由于配置更新涉及重启连接等复杂操作，这里提供简化实现
        logger.info("配置更新请求: {}", configMap);
        return true;
    }

    @Override
    public boolean resetConfig() {
        // 重置配置的简化实现
        logger.info("重置配置请求");
        return true;
    }

    // ====================== 其他工具方法 ======================

    @Override
    public boolean checkSmartCacheIndex() {
        try {
            // 简化实现：检查Redis连接是否正常
            return testConnection();
        } catch (Exception e) {
            logger.error("检查索引失败", e);
            return false;
        }
    }

    @Override
    public String getApplicationName() {
        return redisConfig.getConnectionInfo().getApplicationName();
    }

    // ====================== 私有辅助方法 ======================

    private List<RuleConfig> getRuleConfigs() {
        try {
            // 简化实现：返回一个默认的规则列表
            // 在实际应用中，这里可以从Redis或数据库中读取规则配置
            List<RuleConfig> rules = new ArrayList<>();
            
            // 添加一个默认的全局规则
            RuleConfig defaultRule = new RuleConfig();
            defaultRule.setTtl(Duration.valueOf("300s"));
            rules.add(defaultRule);
            
            return rules;
        } catch (Exception e) {
            logger.error("获取规则配置失败", e);
            return new ArrayList<>();
        }
    }

    private void commitRuleConfigs(List<RuleConfig> rules) {
        try {
            // 简化实现：将规则保存到Redis
            String rulesKey = "smartcache:rules:" + redisConfig.getConnectionInfo().getApplicationName();
            redisTemplate.opsForValue().set(rulesKey, rules);
            logger.info("规则配置已保存，共{}条规则", rules.size());
        } catch (Exception e) {
            logger.error("保存规则配置失败", e);
            throw new RuntimeException("保存规则配置失败: " + e.getMessage(), e);
        }
    }

    private String getIndexName() {
        return redisConfig.getConnectionInfo().getApplicationName() + "-query-idx";
    }

    private String getQueryKey(String queryId) {
        return redisConfig.getConnectionInfo().getApplicationName() + ":query:" + queryId;
    }

    private Optional<RuleConfig> findMatchingRule(QueryInfo queryInfo, List<RuleConfig> rules) {
        return rules.stream()
            .filter(rule -> queryInfo.matchesRule(rule))
            .findFirst();
    }

    private Optional<RuleConfig> findTableMatchingRule(String tableName, List<RuleConfig> rules) {
        return rules.stream()
            .filter(rule -> {
                if (rule.getTablesAny() != null && rule.getTablesAny().contains(tableName)) return true;
                if (rule.getTables() != null && rule.getTables().contains(tableName)) return true;
                if (rule.getTablesAll() != null && rule.getTablesAll().contains(tableName)) return true;
                
                // 检查全局规则
                return (rule.getQueryIds() == null || rule.getQueryIds().isEmpty()) &&
                       (rule.getTables() == null || rule.getTables().isEmpty()) &&
                       (rule.getTablesAny() == null || rule.getTablesAny().isEmpty()) &&
                       (rule.getTablesAll() == null || rule.getTablesAll().isEmpty()) &&
                       (rule.getRegex() == null || rule.getRegex().isEmpty());
            })
            .findFirst();
    }

    private void sortQueries(List<QueryInfo> queries, String sortBy, String sortDirection) {
        if (sortBy == null) return;
        
        Comparator<QueryInfo> comparator = null;
        switch (sortBy.toLowerCase()) {
            case "querytime":
            case "meantime":
                comparator = Comparator.comparingDouble(QueryInfo::getMeanTime);
                break;
            case "accessfrequency":
            case "count":
                comparator = Comparator.comparingLong(QueryInfo::getCount);
                break;
            case "id":
                comparator = Comparator.comparing(QueryInfo::getId);
                break;
        }
        
        if (comparator != null) {
            if ("ASC".equalsIgnoreCase(sortDirection)) {
                queries.sort(comparator);
            } else {
                queries.sort(comparator.reversed());
            }
        }
    }

    private void sortTables(List<TableInfo> tables, String sortBy, String sortDirection) {
        if (sortBy == null) return;
        
        Comparator<TableInfo> comparator = null;
        switch (sortBy.toLowerCase()) {
            case "querytime":
                comparator = Comparator.comparingDouble(TableInfo::getQueryTime);
                break;
            case "accessfrequency":
                comparator = Comparator.comparingLong(TableInfo::getAccessFrequency);
                break;
            case "name":
                comparator = Comparator.comparing(TableInfo::getName);
                break;
        }
        
        if (comparator != null) {
            if ("ASC".equalsIgnoreCase(sortDirection)) {
                tables.sort(comparator);
            } else {
                tables.sort(comparator.reversed());
            }
        }
    }

    private List<QueryInfo> applyPagination(List<QueryInfo> queries, Integer limit, Integer offset) {
        if (offset != null && offset > 0) {
            queries = queries.subList(Math.min(offset, queries.size()), queries.size());
        }
        if (limit != null && limit > 0) {
            queries = queries.subList(0, Math.min(limit, queries.size()));
        }
        return queries;
    }

    private Map<String, String> parseRedisInfo(String info) {
        Map<String, String> result = new HashMap<>();
        String[] lines = info.split("\r\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                result.put(parts[0], parts[1]);
            }
        }
        return result;
    }

    private boolean isValidTtl(String ttl) {
        if (ttl == null || ttl.trim().isEmpty()) {
            return false;
        }
        try {
            Duration.valueOf(ttl);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}