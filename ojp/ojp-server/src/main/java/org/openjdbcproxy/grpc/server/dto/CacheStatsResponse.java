package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 缓存统计响应信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsResponse {
    
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
     * 缓存统计概览
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheOverview {
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
        
        /**
         * 命中率
         */
        private Double hitRate;
        
        /**
         * 缓存项列表
         */
        private List<CacheItem> cacheItems;
    }
    
    /**
     * 缓存项信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheItem {
        /**
         * 缓存键
         */
        private String key;
        
        /**
         * 缓存值大小
         */
        private Long size;
        
        /**
         * 创建时间
         */
        private Long createTime;
        
        /**
         * 最后访问时间
         */
        private Long lastAccessTime;
        
        /**
         * 访问次数
         */
        private Long accessCount;
        
        /**
         * TTL（生存时间）
         */
        private Long ttl;
        
        /**
         * 是否过期
         */
        private Boolean expired;
    }
}
