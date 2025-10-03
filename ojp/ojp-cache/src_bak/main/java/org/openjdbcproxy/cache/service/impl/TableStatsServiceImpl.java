package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.TableStatsResponse;
import org.openjdbcproxy.cache.dto.TableDetailResponse;
import org.openjdbcproxy.cache.dto.QuerySummary;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.CacheQueryRepository;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.AsyncStatsService;
import org.openjdbcproxy.cache.service.TableStatsService;
import org.openjdbcproxy.cache.util.ReadableKeyGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 表格统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableStatsServiceImpl implements TableStatsService {
    private static final String TABLE_STATS_PREFIX = "ojp:table:stats:";

    private final CacheQueryRepository queryRepository;
    private final CacheRuleRepository cacheRuleRepository;
    private final ReadableKeyGenerator readableKeyGenerator;
    
    @Autowired
    private AsyncStatsService asyncStatsService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, List<TableStatsResponse>> getTableStatsList() {
        log.debug("开始获取表格统计列表");
        
        // 获取所有查询摘要
        List<QuerySummary> allQueries = asyncStatsService.findAllSummaries();
        
        // 按数据源和表名分组统计
        Map<String, Map<String, List<QuerySummary>>> groupedByDatasourceAndTable = allQueries.stream()
            .filter(query -> query.getTables() != null && !query.getTables().isEmpty())
            .collect(Collectors.groupingBy(
                QuerySummary::getConnHash,
                Collectors.groupingBy(
                    query -> query.getTables().get(0) // 使用第一个表名作为主表
                )
            ));
        
        Map<String, List<TableStatsResponse>> result = new HashMap<>();
        
        for (Map.Entry<String, Map<String, List<QuerySummary>>> connHashEntry : groupedByDatasourceAndTable.entrySet()) {
            String connHash = connHashEntry.getKey();
            List<TableStatsResponse> tableStats = new ArrayList<>();
            
            for (Map.Entry<String, List<QuerySummary>> tableEntry : connHashEntry.getValue().entrySet()) {
                String tableName = tableEntry.getKey();
                List<QuerySummary> tableQueries = tableEntry.getValue();
                
                TableStatsResponse stats = buildTableStats(tableName, connHash, tableQueries);
                tableStats.add(stats);
            }
            
            result.put(connHash, tableStats);
        }
        
        log.debug("完成表格统计列表获取，数据源数量: {}", result.size());
        return result;
    }

    @Override
    public TableDetailResponse getTableDetails(String tableName, String connHash) {
        log.debug("开始获取表格详细统计，表名: {}, 连接哈希: {}", tableName, connHash);
        
        // 获取相关查询
        List<QuerySummary> relatedQueries;
        if (StringUtils.hasText(connHash)) {
            relatedQueries = queryRepository.findSummariesByTable(tableName, connHash, 0, Integer.MAX_VALUE);
        } else {
            // 如果没有指定数据源，查找所有数据源中的相关查询
            relatedQueries = asyncStatsService.findAllSummaries().stream()
                .filter(query -> query.getTables() != null && query.getTables().contains(tableName))
                .collect(Collectors.toList());
        }
        
        if (relatedQueries.isEmpty()) {
            throw new RuntimeException("Table not found: " + tableName);
        }
        
        // 计算统计数据
        long totalAccessCount = relatedQueries.stream().mapToLong(QuerySummary::getAccessCount).sum();
        double avgQueryTime = relatedQueries.stream()
            .mapToDouble(query -> query.getAvgResponseTime() * query.getAccessCount())
            .sum() / totalAccessCount;
        double maxQueryTime = relatedQueries.stream().mapToDouble(QuerySummary::getAvgResponseTime).max().orElse(0.0);
        double minQueryTime = relatedQueries.stream().mapToDouble(QuerySummary::getAvgResponseTime).min().orElse(0.0);
        double avgCacheHitRate = relatedQueries.stream()
            .mapToDouble(query -> query.getCacheHitRate() * query.getAccessCount())
            .sum() / totalAccessCount;
        
        // 获取当前缓存规则
        String effectiveConnHash = StringUtils.hasText(connHash) ? connHash : 
            relatedQueries.get(0).getConnHash();
        CacheRule currentRule = getCurrentRuleForDetail(tableName, effectiveConnHash);
        
        // 构建相关查询列表
        List<TableDetailResponse.RelatedQuery> relatedQueryList = relatedQueries.stream()
            .map(query -> TableDetailResponse.RelatedQuery.builder()
                .queryId(query.getQueryId())
                .accessCount(query.getAccessCount())
                .avgTime(query.getAvgResponseTime())
                .cached(query.getCacheHitRate() > 0)
                .build())
            .collect(Collectors.toList());
        
        // 构建性能历史记录（模拟数据）
        List<TableDetailResponse.PerformanceHistory> performanceHistory = buildPerformanceHistory(
            avgQueryTime, totalAccessCount, avgCacheHitRate);
        
        TableDetailResponse response = TableDetailResponse.builder()
            .name(tableName)
            .accessFrequency(totalAccessCount)
            .avgQueryTime(avgQueryTime)
            .maxQueryTime(maxQueryTime)
            .minQueryTime(minQueryTime)
            .cacheHitRate(avgCacheHitRate)
            .totalCacheSize(calculateCacheSize(relatedQueries))
            .currentRule(currentRule)
            .relatedQueries(relatedQueryList)
            .performanceHistory(performanceHistory)
            .build();
        
        log.debug("完成表格详细统计获取，表名: {}", tableName);
        return response;
    }
    
    private TableStatsResponse buildTableStats(String tableName, String connHash, List<QuerySummary> queries) {
        long totalAccessCount = queries.stream()
            .mapToLong(q -> q.getAccessCount() != null ? q.getAccessCount() : 0L)
            .sum();
        
        double avgQueryTime = 0.0;
        if (totalAccessCount > 0) {
            avgQueryTime = queries.stream()
                .filter(q -> q.getAvgResponseTime() != null && q.getAccessCount() != null)
                .mapToDouble(query -> query.getAvgResponseTime() * query.getAccessCount())
                .sum() / totalAccessCount;
        }
        
        double avgCacheHitRate = 0.0;
        if (totalAccessCount > 0) {
            avgCacheHitRate = queries.stream()
                .filter(q -> q.getCacheHitRate() != null && q.getAccessCount() != null)
                .mapToDouble(query -> query.getCacheHitRate() * query.getAccessCount())
                .sum() / totalAccessCount;
        }
        
        CacheRule currentRule = getCurrentRuleSimple(tableName, connHash);
        
        List<String> relatedQueryIds = queries.stream()
            .map(QuerySummary::getQueryId)
            .collect(Collectors.toList());
        
        return TableStatsResponse.builder()
            .name(tableName)
            .accessFrequency(totalAccessCount)
            .avgQueryTime(avgQueryTime)
            .cacheHitRate(avgCacheHitRate)
            .currentRule(currentRule)
            .relatedQueries(relatedQueryIds)
            .build();
    }
    
    private CacheRule getCurrentRuleSimple(String tableName, String connHash) {
        List<CacheRule> rules = cacheRuleRepository.findByTable(tableName, connHash);
        if (rules.isEmpty()) {
            return null;
        }
        
        CacheRule rule = rules.get(0);
        return CacheRule.builder()
            .enabled(rule.isEnabled())
            .ttl(rule.getTtl())
            .build();
    }
    
    private TableStatsResponse getTableStatsFromRedis(String connHash, String tableName) {
        try {
            // connHash由extractConnHash方法保证非空，直接使用
        String readableConnId = "conn_" + connHash;
            String key = TABLE_STATS_PREFIX + readableConnId + ":" + tableName;
            return (TableStatsResponse) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("从Redis获取表统计失败: datasource={}, tableName={}", connHash, tableName, e);
            return null;
        }
    }
    
    private CacheRule getCurrentRuleForDetail(String tableName, String datasource) {
        List<CacheRule> rules = cacheRuleRepository.findByTable(tableName, datasource);
        if (rules.isEmpty()) {
            return null;
        }
        
        return rules.get(0); // 直接返回第一个匹配的规则
    }
    
    private long calculateCacheSize(List<QuerySummary> queries) {
        // 简单估算：每个查询平均缓存大小 * 访问次数 * 命中率
        return queries.stream()
            .mapToLong(query -> (long)(1024 * query.getAccessCount() * query.getCacheHitRate()))
            .sum();
    }
    
    private List<TableDetailResponse.PerformanceHistory> buildPerformanceHistory(
            double avgQueryTime, long accessCount, double cacheHitRate) {
        List<TableDetailResponse.PerformanceHistory> history = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 生成最近24小时的模拟数据
        for (int i = 23; i >= 0; i--) {
            LocalDateTime timestamp = now.minusHours(i);
            double timeVariation = 0.8 + (Math.random() * 0.4); // 80%-120%的变化
            long countVariation = (long)(accessCount * (0.8 + Math.random() * 0.4) / 24);
            double hitRateVariation = Math.max(0, Math.min(1, cacheHitRate * (0.9 + Math.random() * 0.2)));
            
            history.add(TableDetailResponse.PerformanceHistory.builder()
                .timestamp(timestamp)
                .avgQueryTime(avgQueryTime * timeVariation)
                .accessCount(countVariation)
                .cacheHitRate(hitRateVariation)
                .build());
        }
        
        return history;
    }
}