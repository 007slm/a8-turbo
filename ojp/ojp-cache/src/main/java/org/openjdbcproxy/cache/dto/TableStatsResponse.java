package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表格统计响应
 * 用于表格统计列表展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatsResponse {

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
     * 缓存命中率
     */
    private Double cacheHitRate;

    /**
     * 当前缓存规则
     */
    private CurrentRule currentRule;

    /**
     * 相关查询ID列表
     */
    private List<String> relatedQueries;

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
        private Boolean enabled;
    }
}