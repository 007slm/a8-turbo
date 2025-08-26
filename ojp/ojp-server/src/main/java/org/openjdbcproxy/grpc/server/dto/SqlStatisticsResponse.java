package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SQL统计响应信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatisticsResponse {
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private Object data;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * SQL统计概览
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsOverview {
        /**
         * 总查询数
         */
        private Long totalQueries;
        
        /**
         * 缓存命中数
         */
        private Long cacheHits;
        
        /**
         * 缓存未命中数
         */
        private Long cacheMisses;
        
        /**
         * 命中率
         */
        private Double hitRate;
        
        /**
         * 平均查询时间
         */
        private Double avgQueryTime;
        
        /**
         * 慢查询数
         */
        private Long slowQueries;
        
        /**
         * 缓存统计
         */
        private CacheStats cacheStats;
    }
    
    /**
     * 缓存统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheStats {
        /**
         * 总缓存数
         */
        private Long totalCaches;
        
        /**
         * 活跃缓存数
         */
        private Long activeCaches;
        
        /**
         * 内存使用率
         */
        private Double memoryUsagePercent;
        
        /**
         * 平均响应时间
         */
        private Double avgResponseTime;
    }
    
    /**
     * SQL查询统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlQueryStats {
        /**
         * 查询ID
         */
        private String queryId;
        
        /**
         * SQL语句
         */
        private String sql;
        
        /**
         * 执行次数
         */
        private Long executionCount;
        
        /**
         * 平均执行时间
         */
        private Double avgExecutionTime;
        
        /**
         * 总执行时间
         */
        private Long totalExecutionTime;
        
        /**
         * 是否被缓存
         */
        private Boolean isCached;
        
        /**
         * 缓存命中次数
         */
        private Long cacheHits;
        
        /**
         * 最后执行时间
         */
        private Long lastExecutionTime;
    }
    
    /**
     * 表格统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableStats {
        /**
         * 表格名称
         */
        private String tableName;
        
        /**
         * 访问次数
         */
        private Long accessCount;
        
        /**
         * 平均查询时间
         */
        private Double avgQueryTime;
        
        /**
         * 是否被缓存
         */
        private Boolean isCached;
        
        /**
         * 缓存命中次数
         */
        private Long cacheHits;
        
        /**
         * 最后访问时间
         */
        private Long lastAccessTime;
    }
}
