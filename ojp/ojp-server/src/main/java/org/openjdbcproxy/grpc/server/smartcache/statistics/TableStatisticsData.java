package org.openjdbcproxy.grpc.server.smartcache.statistics;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 表统计数据
 * 用于存储表访问的统计信息，支持缓存规则引擎使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatisticsData {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 访问频率（执行次数）
     */
    private long accessFrequency;
    
    /**
     * 平均查询时间（毫秒）
     */
    private double averageQueryTime;
    
    /**
     * 最大查询时间（毫秒）
     */
    private double maxQueryTime;
    
    /**
     * 最小查询时间（毫秒）
     */
    private double minQueryTime;
    
    /**
     * 总查询时间（毫秒）
     */
    private double totalQueryTime;
    
    /**
     * 最后访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessTime;
    
    /**
     * 首次访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstAccessTime;
    
    /**
     * 是否被缓存规则覆盖
     */
    private boolean isCached;
    
    /**
     * 当前缓存TTL（秒）
     */
    private long currentTtl;
    
    /**
     * 缓存命中次数
     */
    private long cacheHitCount;
    
    /**
     * 缓存未命中次数
     */
    private long cacheMissCount;
    
    /**
     * 涉及该表的SQL查询数量
     */
    private long relatedQueryCount;
    
    /**
     * 计算缓存命中率
     */
    public double getCacheHitRate() {
        long totalCacheAccess = cacheHitCount + cacheMissCount;
        return totalCacheAccess > 0 ? (double) cacheHitCount / totalCacheAccess : 0.0;
    }
    
    /**
     * 更新访问统计
     */
    public void updateAccessStats(double queryTime) {
        this.accessFrequency++;
        this.totalQueryTime += queryTime;
        this.averageQueryTime = this.totalQueryTime / this.accessFrequency;
        
        if (this.maxQueryTime < queryTime) {
            this.maxQueryTime = queryTime;
        }
        
        if (this.minQueryTime == 0 || this.minQueryTime > queryTime) {
            this.minQueryTime = queryTime;
        }
        
        this.lastAccessTime = LocalDateTime.now();
        
        if (this.firstAccessTime == null) {
            this.firstAccessTime = LocalDateTime.now();
        }
    }
    
    /**
     * 更新缓存统计
     */
    public void updateCacheStats(boolean isHit) {
        if (isHit) {
            this.cacheHitCount++;
        } else {
            this.cacheMissCount++;
        }
    }
    
    /**
     * 增加相关查询计数
     */
    public void incrementRelatedQueryCount() {
        this.relatedQueryCount++;
    }
}
