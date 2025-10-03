package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openjdbcproxy.cache.entity.CacheRule;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表格详细统计响应
 * 用于表格详细统计页面展示
 *
 * 字段说明:
 * name: 表名
 * accessFrequency: 访问频率
 * avgQueryTime: 平均查询时间（毫秒）
 * maxQueryTime: 最大查询时间（毫秒）
 * minQueryTime: 最小查询时间（毫秒）
 * cacheHitRate: 缓存命中率
 * totalCacheSize: 总缓存大小（字节）
 * currentRule: 当前缓存规则
 * relatedQueries: 相关查询列表
 * performanceHistory: 性能历史记录
 *
 * RelatedQuery内部类字段说明:
 * queryId: 查询ID
 * accessCount: 访问次数
 * avgTime: 平均时间
 * cached: 是否缓存
 *
 * PerformanceHistory内部类字段说明:
 * timestamp: 时间戳
 * avgQueryTime: 平均查询时间
 * accessCount: 访问次数
 * cacheHitRate: 缓存命中率
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDetailResponse {

    private String name;
    private Long accessFrequency;
    private Double avgQueryTime;
    private Double maxQueryTime;
    private Double minQueryTime;
    private Double cacheHitRate;
    private Long totalCacheSize;
    private CacheRule currentRule;
    private List<RelatedQuery> relatedQueries;
    private List<PerformanceHistory> performanceHistory;



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