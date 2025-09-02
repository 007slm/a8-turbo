package org.openjdbcproxy.grpc.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存统计相关的DTO类
 */
public class CacheStatsDto {
    
    /**
     * 缓存概览统计
     */
    @Data
    public static class OverviewStats {
        private int totalCaches;
        private int activeCaches;
        private long totalKeys;
        private double memoryUsage;
        private double hitRate;
        private double avgResponseTime;
    }
    
    /**
     * 缓存命中率统计
     */
    @Data
    public static class HitRateStats {
        private double currentRate;
        private double averageRate;
        private double maxRate;
        private String trend; // "up", "down", "stable"
        private List<TimeSeriesPoint> timeSeries;
    }
    
    /**
     * 查询性能统计
     */
    @Data
    public static class QueryPerformanceStats {
        private double avgQueryTime;
        private double avgCachedQueryTime;
        private double avgNonCachedQueryTime;
        private double performanceImprovement;
    }
    
    /**
     * 热门表格统计
     */
    @Data
    public static class TopTableStats {
        private String name;
        private long accessFrequency;
        private double avgQueryTime;
        private boolean cached;
        private String ttl;
    }
    
    /**
     * 慢查询统计
     */
    @Data
    public static class SlowQueryStats {
        private String sql;
        private long executionTime;
        private int count;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastExecuted;
    }
    
    /**
     * 查询信息
     */
    @Data
    public static class QueryInfo {
        private String queryId;
        private String sql;
        private List<String> tables;
        private long count;
        private double meanQueryTime;
        private boolean cached;
        private String currentTtl;
        private String description;
    }
    
    /**
     * 表格信息
     */
    @Data
    public static class TableInfo {
        private String name;
        private String ttl;
        private double avgQueryTime;
        private long accessFrequency;
        private boolean cached;
        private List<String> relatedTables;
    }
    
    /**
     * 时间序列数据点
     */
    @Data
    public static class TimeSeriesPoint {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;
        private double value;
        private String label;
    }
    
    /**
     * 规则验证结果
     */
    @Data
    public static class RuleValidationResult {
        private boolean valid;
        private String message;
        private List<String> errors;
        private List<String> warnings;
    }
}
