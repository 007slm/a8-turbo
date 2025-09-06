package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存命中率统计数据传输对象
 * 提供按时间周期聚合的命中率统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HitRateStats {
    
    /**
     * 统计周期
     */
    private String period;
    
    /**
     * 数据源名称
     */
    private String datasource;
    
    /**
     * 当前命中率
     */
    private Double currentHitRate;
    
    /**
     * 上一周期命中率
     */
    private Double previousHitRate;
    
    /**
     * 趋势（increasing/decreasing/stable）
     */
    private String trend;
    
    /**
     * 历史命中率数据
     */
    private List<HitRateHistoryItem> history;
    
    /**
     * 命中率历史数据项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HitRateHistoryItem {
        
        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
        
        /**
         * 命中率
         */
        private Double hitRate;
        
        /**
         * 总请求数
         */
        private Long totalRequests;
        
        /**
         * 缓存命中数
         */
        private Long cacheHits;
    }
}