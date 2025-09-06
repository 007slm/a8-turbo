package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能统计数据传输对象
 * 提供查询响应时间、吞吐量等性能指标统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStats {
    
    /**
     * 统计周期
     */
    private String period;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;
    
    /**
     * 最大响应时间（毫秒）
     */
    private Double maxResponseTime;
    
    /**
     * 最小响应时间（毫秒）
     */
    private Double minResponseTime;
    
    /**
     * 吞吐量（请求/秒）
     */
    private Double throughput;
    
    /**
     * 缓存大小（字节）
     */
    private Long cacheSize;
    
    /**
     * 性能历史数据
     */
    private List<PerformanceHistoryItem> performanceHistory;
    
    /**
     * 性能历史数据项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceHistoryItem {
        
        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
        
        /**
         * 平均响应时间（毫秒）
         */
        private Double avgResponseTime;
        
        /**
         * 吞吐量（请求/秒）
         */
        private Double throughput;
        
        /**
         * 缓存大小（字节）
         */
        private Long cacheSize;
    }
}