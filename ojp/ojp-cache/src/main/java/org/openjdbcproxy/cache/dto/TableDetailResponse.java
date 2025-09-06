package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表格详细统计响应
 * 用于表格详细统计页面展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDetailResponse {

    /**
     * 表名
     */
    private String name;

    /**
     * 访问频率
     */
    private Long accessFrequency;

    /**
     * 平均查询时间（毫秒）
     */
    private Double avgQueryTime;

    /**
     * 最大查询时间（毫秒）
     */
    private Double maxQueryTime;

    /**
     * 最小查询时间（毫秒）
     */
    private Double minQueryTime;

    /**
     * 缓存命中率
     */
    private Double cacheHitRate;

    /**
     * 总缓存大小（字节）
     */
    private Long totalCacheSize;

    /**
     * 当前缓存规则
     */
    private CurrentRule currentRule;

    /**
     * 相关查询列表
     */
    private List<RelatedQuery> relatedQueries;

    /**
     * 性能历史记录
     */
    private List<PerformanceHistory> performanceHistory;

    /**
     * 当前缓存规则信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentRule {
        private String id;
        private Integer ttl;
        private String ruleType;
        private List<String> tables;
        private Integer priority;
        private Boolean enabled;
        private String description;
    }

    /**
     * 相关查询信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedQuery {
        private String queryId;
        private Long accessCount;
        private Double avgTime;
        private Boolean cached;
    }

    /**
     * 性能历史记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceHistory {
        private LocalDateTime timestamp;
        private Double avgQueryTime;
        private Long accessCount;
        private Double cacheHitRate;
    }
}