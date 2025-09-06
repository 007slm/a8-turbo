package org.openjdbcproxy.cache.repository.impl;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.dto.QuerySummary;
import org.openjdbcproxy.cache.repository.QueryRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于Redis的查询数据存储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisQueryRepository implements QueryRepository {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis键前缀
    private static final String QUERY_PREFIX = "fluxdb:query:";
    private static final String QUERY_HASH_PREFIX = "fluxdb:query_hash:";
    private static final String QUERY_LIST_PREFIX = "fluxdb:query_list:";
    private static final String QUERY_STATS_PREFIX = "fluxdb:query_stats:";
    private static final String QUERY_TABLE_PREFIX = "fluxdb:query_table:";

    // 默认过期时间（7天）
    private static final long DEFAULT_EXPIRE_DAYS = 7;

    @Override
    public Query save(Query query) {
        try {
            String queryKey = QUERY_PREFIX + query.getQueryId();
            String hashKey = QUERY_HASH_PREFIX + query.getDatasourceName() + ":" + query.getParameterHash();
            String listKey = QUERY_LIST_PREFIX + query.getDatasourceName();
            
            // 保存查询详情
            String queryJson = JSON.toJSONString(query);
            redisTemplate.opsForValue().set(queryKey, queryJson, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            
            // 保存哈希索引
            redisTemplate.opsForValue().set(hashKey, query.getQueryId(), DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            
            // 添加到查询列表
            redisTemplate.opsForZSet().add(listKey, query.getQueryId(), System.currentTimeMillis());
            
            // 保存表关联索引
            if (query.getTables() != null) {
                for (String table : query.getTables()) {
                    String tableKey = QUERY_TABLE_PREFIX + query.getDatasourceName() + ":" + table;
                    redisTemplate.opsForSet().add(tableKey, query.getQueryId());
                    redisTemplate.expire(tableKey, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }
            
            log.debug("Saved query: {}", query.getQueryId());
            return query;
        } catch (Exception e) {
            log.error("Failed to save query: {}", query.getQueryId(), e);
            throw new RuntimeException("Failed to save query", e);
        }
    }

    @Override
    public Optional<Query> findById(String queryId) {
        try {
            String queryKey = QUERY_PREFIX + queryId;
            String queryJson = redisTemplate.opsForValue().get(queryKey);
            
            if (queryJson == null) {
                return Optional.empty();
            }
            
            Query query = JSON.parseObject(queryJson, Query.class);
            return Optional.of(query);
        } catch (Exception e) {
            log.error("Failed to find query by id: {}", queryId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Query> findBySqlHash(String sqlHash, String datasourceName) {
        try {
            String hashKey = QUERY_HASH_PREFIX + datasourceName + ":" + sqlHash;
            String queryId = redisTemplate.opsForValue().get(hashKey);
            
            if (queryId == null) {
                return Optional.empty();
            }
            
            return findById(queryId);
        } catch (Exception e) {
            log.error("Failed to find query by sql hash: {} for datasource: {}", sqlHash, datasourceName, e);
            return Optional.empty();
        }
    }

    @Override
    public List<QuerySummary> findAllSummaries(int offset, int limit) {
        try {
            Set<String> datasources = getDatasources();
            List<QuerySummary> allSummaries = new ArrayList<>();
            
            for (String datasource : datasources) {
                List<QuerySummary> summaries = findSummariesByDatasource(datasource, 0, Integer.MAX_VALUE);
                allSummaries.addAll(summaries);
            }
            
            // 按最后访问时间排序
            allSummaries.sort((a, b) -> {
                if (a.getLastAccessTime() == null && b.getLastAccessTime() == null) return 0;
                if (a.getLastAccessTime() == null) return 1;
                if (b.getLastAccessTime() == null) return -1;
                return b.getLastAccessTime().compareTo(a.getLastAccessTime());
            });
            
            return allSummaries.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find all query summaries", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<QuerySummary> findSummariesByDatasource(String datasourceName, int offset, int limit) {
        try {
            String listKey = QUERY_LIST_PREFIX + datasourceName;
            Set<String> queryIds = redisTemplate.opsForZSet().reverseRange(listKey, offset, offset + limit - 1);
            
            if (queryIds == null || queryIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<QuerySummary> summaries = new ArrayList<>();
            for (String queryId : queryIds) {
                Optional<Query> queryOpt = findById(queryId);
                if (queryOpt.isPresent()) {
                    Query query = queryOpt.get();
                    QuerySummary summary = convertToSummary(query);
                    summaries.add(summary);
                }
            }
            
            return summaries;
        } catch (Exception e) {
            log.error("Failed to find query summaries by datasource: {}", datasourceName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<QuerySummary> findSummariesByTable(String tableName, String datasourceName, int offset, int limit) {
        try {
            String tableKey = QUERY_TABLE_PREFIX + datasourceName + ":" + tableName;
            Set<String> queryIds = redisTemplate.opsForSet().members(tableKey);
            
            if (queryIds == null || queryIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<QuerySummary> summaries = new ArrayList<>();
            for (String queryId : queryIds) {
                Optional<Query> queryOpt = findById(queryId);
                if (queryOpt.isPresent()) {
                    Query query = queryOpt.get();
                    QuerySummary summary = convertToSummary(query);
                    summaries.add(summary);
                }
            }
            
            // 按最后访问时间排序并分页
            return summaries.stream()
                    .sorted((a, b) -> {
                        if (a.getLastAccessTime() == null && b.getLastAccessTime() == null) return 0;
                        if (a.getLastAccessTime() == null) return 1;
                        if (b.getLastAccessTime() == null) return -1;
                        return b.getLastAccessTime().compareTo(a.getLastAccessTime());
                    })
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to find query summaries by table: {} for datasource: {}", tableName, datasourceName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void updateStatistics(String queryId, long responseTime, boolean cacheHit) {
        try {
            Optional<Query> queryOpt = findById(queryId);
            if (queryOpt.isPresent()) {
                Query query = queryOpt.get();
                query.updateStatistics(responseTime, cacheHit);
                save(query);
                log.debug("Updated statistics for query: {}", queryId);
            }
        } catch (Exception e) {
            log.error("Failed to update statistics for query: {}", queryId, e);
        }
    }

    @Override
    public void updateLastAccessTime(String queryId, LocalDateTime lastAccessTime) {
        try {
            Optional<Query> queryOpt = findById(queryId);
            if (queryOpt.isPresent()) {
                Query query = queryOpt.get();
                query.setLastAccessTime(lastAccessTime);
                save(query);
                log.debug("Updated last access time for query: {}", queryId);
            }
        } catch (Exception e) {
            log.error("Failed to update last access time for query: {}", queryId, e);
        }
    }

    @Override
    public boolean deleteById(String queryId) {
        try {
            Optional<Query> queryOpt = findById(queryId);
            if (!queryOpt.isPresent()) {
                return false;
            }
            
            Query query = queryOpt.get();
            String queryKey = QUERY_PREFIX + queryId;
            String hashKey = QUERY_HASH_PREFIX + query.getDatasourceName() + ":" + query.getParameterHash();
            String listKey = QUERY_LIST_PREFIX + query.getDatasourceName();
            
            // 删除查询详情
            redisTemplate.delete(queryKey);
            
            // 删除哈希索引
            redisTemplate.delete(hashKey);
            
            // 从查询列表中移除
            redisTemplate.opsForZSet().remove(listKey, queryId);
            
            // 从表关联索引中移除
            if (query.getTables() != null) {
                for (String table : query.getTables()) {
                    String tableKey = QUERY_TABLE_PREFIX + query.getDatasourceName() + ":" + table;
                    redisTemplate.opsForSet().remove(tableKey, queryId);
                }
            }
            
            log.debug("Deleted query: {}", queryId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete query: {}", queryId, e);
            return false;
        }
    }

    @Override
    public int deleteByDatasource(String datasourceName) {
        try {
            String listKey = QUERY_LIST_PREFIX + datasourceName;
            Set<String> queryIds = redisTemplate.opsForZSet().range(listKey, 0, -1);
            
            if (queryIds == null || queryIds.isEmpty()) {
                return 0;
            }
            
            int deletedCount = 0;
            for (String queryId : queryIds) {
                if (deleteById(queryId)) {
                    deletedCount++;
                }
            }
            
            // 清空查询列表
            redisTemplate.delete(listKey);
            
            log.info("Deleted {} queries for datasource: {}", deletedCount, datasourceName);
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to delete queries by datasource: {}", datasourceName, e);
            return 0;
        }
    }

    @Override
    public long count() {
        try {
            Set<String> datasources = getDatasources();
            long totalCount = 0;
            
            for (String datasource : datasources) {
                totalCount += countByDatasource(datasource);
            }
            
            return totalCount;
        } catch (Exception e) {
            log.error("Failed to count queries", e);
            return 0;
        }
    }

    @Override
    public long countByDatasource(String datasourceName) {
        try {
            String listKey = QUERY_LIST_PREFIX + datasourceName;
            Long count = redisTemplate.opsForZSet().count(listKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to count queries by datasource: {}", datasourceName, e);
            return 0;
        }
    }

    @Override
    public List<QuerySummary> findRecentlyAccessed(String datasourceName, int limit) {
        if (datasourceName != null) {
            return findSummariesByDatasource(datasourceName, 0, limit);
        } else {
            return findAllSummaries(0, limit);
        }
    }

    @Override
    public List<QuerySummary> findMostFrequent(String datasourceName, int limit) {
        List<QuerySummary> summaries = datasourceName != null 
                ? findSummariesByDatasource(datasourceName, 0, Integer.MAX_VALUE)
                : findAllSummaries(0, Integer.MAX_VALUE);
        
        return summaries.stream()
                .sorted((a, b) -> Long.compare(b.getAccessCount() != null ? b.getAccessCount() : 0, 
                                              a.getAccessCount() != null ? a.getAccessCount() : 0))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuerySummary> findHighestCacheHitRate(String datasourceName, int limit) {
        List<QuerySummary> summaries = datasourceName != null 
                ? findSummariesByDatasource(datasourceName, 0, Integer.MAX_VALUE)
                : findAllSummaries(0, Integer.MAX_VALUE);
        
        return summaries.stream()
                .filter(s -> s.getCacheHitRate() != null && s.getCacheHitRate() > 0)
                .sorted((a, b) -> Double.compare(b.getCacheHitRate(), a.getCacheHitRate()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public int batchSave(List<Query> queries) {
        int savedCount = 0;
        for (Query query : queries) {
            try {
                save(query);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save query in batch: {}", query.getQueryId(), e);
            }
        }
        return savedCount;
    }

    @Override
    public int cleanupExpiredQueries(LocalDateTime expiredBefore) {
        // Redis会自动处理过期键，这里可以实现额外的清理逻辑
        log.info("Cleanup expired queries before: {}", expiredBefore);
        return 0;
    }

    /**
     * 获取所有数据源名称
     */
    private Set<String> getDatasources() {
        try {
            Set<String> keys = redisTemplate.keys(QUERY_LIST_PREFIX + "*");
            if (keys == null) {
                return Collections.emptySet();
            }
            
            return keys.stream()
                    .map(key -> key.substring(QUERY_LIST_PREFIX.length()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get datasources", e);
            return Collections.emptySet();
        }
    }

    /**
     * 将Query对象转换为QuerySummary
     */
    private QuerySummary convertToSummary(Query query) {
        return QuerySummary.builder()
                .queryId(query.getQueryId())
                .sql(query.getSql())
                .tables(query.getTables())
                .accessCount(query.getAccessCount())
                .avgResponseTime(query.getAvgResponseTime())
                .cacheHitRate(query.getCacheHitRate())
                .hasCache(query.getCacheHitCount() != null && query.getCacheHitCount() > 0)
                .lastAccessTime(query.getLastAccessTime())
                .datasourceName(query.getDatasourceName())
                .queryType(query.getQueryType() != null ? query.getQueryType().name() : "UNKNOWN")
                .cacheStatus(determineCacheStatus(query))
                .build();
    }

    /**
     * 确定缓存状态
     */
    private QuerySummary.CacheStatus determineCacheStatus(Query query) {
        if (query.getCacheHitCount() == null || query.getCacheHitCount() == 0) {
            return QuerySummary.CacheStatus.NOT_CACHED;
        }
        
        // 这里可以添加更复杂的缓存状态判断逻辑
        return QuerySummary.CacheStatus.CACHED;
    }
}