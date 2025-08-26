package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存统计数据模型
 * 所有返回值都使用强类型，避免使用Object
 */
public class CacheStatsDto {

    /**
     * 缓存概览统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheOverview {
        private Long totalRequests;
        private Long cacheHits;
        private Long cacheMisses;
        private Double hitRate;
        private Long activeRules;
        private Long cacheSize;
        private Long totalQueries;
        private Long slowQueries;
        private LocalDateTime lastUpdated;
    }

    /**
     * 缓存命中率统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HitRateStats {
        private List<HourlyHitRate> hourlyStats;
        private Double averageHitRate;
        private Long totalRequests;
        private Long totalHits;
        private Long totalMisses;
    }

    /**
     * 小时命中率统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyHitRate {
        private String hour;
        private Long requests;
        private Long hits;
        private Long misses;
        private Double hitRate;
        private Double averageResponseTime;
    }

    /**
     * 查询性能统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryPerformanceStats {
        private List<QueryInfo> queries;
        private Long totalQueries;
        private Double averageExecutionTime;
        private Long slowQueries;
        private Long cachedQueries;
    }

    /**
     * 查询信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryInfo {
        private String queryId;
        private String sql;
        private String tableName;
        private Long executionCount;
        private Double averageExecutionTime;
        private Double maxExecutionTime;
        private Double minExecutionTime;
        private Long totalExecutionTime;
        private LocalDateTime lastExecuted;
        private Boolean isCached;
        private Long cacheHitCount;
        private Long cacheMissCount;
        private String queryType;
        private String currentTtl;
    }

    /**
     * 热门表格统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularTablesStats {
        private List<TableInfo> tables;
        private Long totalTables;
        private Long totalQueries;
        private Double averageQueryTime;
    }

    /**
     * 表格信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfo {
        private String tableName;
        private Long accessFrequency;
        private Double averageQueryTime;
        private Double maxQueryTime;
        private Double minQueryTime;
        private Long totalQueryTime;
        private LocalDateTime lastAccessTime;
        private LocalDateTime firstAccessTime;
        private Boolean isCached;
        private String currentTtl;
        private Long cacheHitCount;
        private Long cacheMissCount;
        private Long relatedQueryCount;
    }

    /**
     * 慢查询统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlowQueryStats {
        private List<SlowQueryInfo> slowQueries;
        private Long totalSlowQueries;
        private Double threshold;
        private Double averageSlowQueryTime;
    }

    /**
     * 慢查询信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlowQueryInfo {
        private String queryId;
        private String sql;
        private String tableName;
        private Long executionCount;
        private Double averageExecutionTime;
        private Double maxExecutionTime;
        private LocalDateTime lastExecuted;
        private Boolean isCached;
        private String currentTtl;
        private String queryType;
    }

    /**
     * 查询列表响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryListResponse {
        private List<QueryInfo> queries;
        private Long totalCount;
        private Integer page;
        private Integer size;
        private String tableName;
    }

    /**
     * 表格列表响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableListResponse {
        private List<TableInfo> tables;
        private Long totalCount;
        private Integer page;
        private Integer size;
    }

    /**
     * 缓存规则信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheRuleInfo {
        private String ruleId;
        private String name;
        private String ruleType;
        private String pattern;
        private String ttl;
        private Integer priority;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String description;
        private Long matchCount;
        private Long hitCount;
        private Double hitRate;
    }

    /**
     * 缓存规则列表响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheRuleListResponse {
        private List<CacheRuleInfo> rules;
        private Long totalCount;
        private String queryId;
        private String tableName;
    }

    /**
     * 创建缓存规则请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCacheRuleRequest {
        private String name;
        private String ruleType;
        private String pattern;
        private String ttl;
        private Integer priority;
        private String description;
    }

    /**
     * 表格统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableStatsInfo {
        private String tableName;
        private Long queryCount;
        private Double cacheHitRate;
        private Double averageExecutionTime;
        private Double maxExecutionTime;
        private Double minExecutionTime;
        private Long totalExecutionTime;
        private LocalDateTime lastUpdated;
        private LocalDateTime firstAccessTime;
        private Boolean isCached;
        private String currentTtl;
        private Long cacheHitCount;
        private Long cacheMissCount;
        private List<QueryInfo> relatedQueries;
    }
}
