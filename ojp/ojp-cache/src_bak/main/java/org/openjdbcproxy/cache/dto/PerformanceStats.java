package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能统计数据传输对象
 * 提供查询响应时间、吞吐量等性能指标统计
 * 
 * 字段说明:
 * period: 统计周期
 * avgResponseTime: 平均响应时间（毫秒）
 * maxResponseTime: 最大响应时间（毫秒）
 * minResponseTime: 最小响应时间（毫秒）
 * throughput: 吞吐量（请求/秒）
 * cacheSize: 缓存大小（字节）
 * performanceHistory: 性能历史数据
 * 
 * PerformanceHistoryItem内部类字段说明:
 * timestamp: 时间戳
 * avgResponseTime: 平均响应时间（毫秒）
 * throughput: 吞吐量（请求/秒）
 * cacheSize: 缓存大小（字节）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStats {
    
    private String period;
    private Double avgResponseTime;
    private Double maxResponseTime;
    private Double minResponseTime;
    private Double throughput;
    private Long cacheSize;
    private List<PerformanceHistoryItem> performanceHistory;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceHistoryItem {
        private LocalDateTime timestamp;
        private Double avgResponseTime;
        private Double throughput;
        private Long cacheSize;
    }
}