package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openjdbcproxy.cache.entity.CacheRule;

import java.util.List;

/**
 * 表格统计响应
 * 用于表格统计列表展示
 *
 * 字段说明:
 * name: 表名
 * accessFrequency: 访问频率
 * avgQueryTime: 平均查询时间（毫秒）
 * cacheHitRate: 缓存命中率
 * currentRule: 当前缓存规则
 * relatedQueries: 相关查询ID列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatsResponse {

    private String name;
    private Long accessFrequency;
    private Double avgQueryTime;
    private Double cacheHitRate;
    private CacheRule currentRule;
    private List<String> relatedQueries;
}