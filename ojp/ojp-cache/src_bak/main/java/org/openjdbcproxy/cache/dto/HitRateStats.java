package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存命中率统计数据传输对象
 * 提供按时间周期聚合的命中率统计信息
 *
 * 字段说明:
 * period: 统计周期
 * datasource: 数据源名称
 * currentHitRate: 当前命中率
 * previousHitRate: 上一周期命中率
 * trend: 趋势（increasing/decreasing/stable）
 * history: 历史命中率数据
 *
 * HitRateHistoryItem内部类字段说明:
 * timestamp: 时间戳
 * hitRate: 命中率
 * totalRequests: 总请求数
 * cacheHits: 缓存命中数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HitRateStats {
    
    private String period;
    private String datasource;
    private Double currentHitRate;
    private Double previousHitRate;
    private String trend;
    private List<HitRateHistoryItem> history;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HitRateHistoryItem {
        private LocalDateTime timestamp;
        private Double hitRate;
        private Long totalRequests;
        private Long cacheHits;
    }
}