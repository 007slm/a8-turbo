package org.openjdbcproxy.grpc.server.smartcache.statistics;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * SQL统计数据
 * 用于存储SQL执行的统计信息，支持缓存规则引擎使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlStatisticsData {
    
    /**
     * SQL查询ID（基于SQL内容的哈希值）
     */
    private String queryId;
    
    /**
     * 原始SQL语句
     */
    private String sql;
    
    /**
     * SQL涉及的表名集合
     */
    private Set<String> tables;
    
    /**
     * 执行次数
     */
    private long executionCount;
    
    /**
     * 平均执行时间（毫秒）
     */
    private double averageExecutionTime;
    
    /**
     * 最大执行时间（毫秒）
     */
    private double maxExecutionTime;
    
    /**
     * 最小执行时间（毫秒）
     */
    private double minExecutionTime;
    
    /**
     * 总执行时间（毫秒）
     */
    private double totalExecutionTime;
    
    /**
     * 最后执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutionTime;
    
    /**
     * 首次执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstExecutionTime;
    
    /**
     * 查询类型（SELECT, INSERT, UPDATE, DELETE等）
     */
    private String queryType;
    
    /**
     * 是否被缓存
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
     * 计算缓存命中率
     */
    public double getCacheHitRate() {
        long totalCacheAccess = cacheHitCount + cacheMissCount;
        return totalCacheAccess > 0 ? (double) cacheHitCount / totalCacheAccess : 0.0;
    }
    
    /**
     * 获取表名字符串（逗号分隔）
     */
    public String getTablesString() {
        return tables != null ? String.join(",", tables) : "";
    }
    
    /**
     * 更新执行统计
     */
    public void updateExecutionStats(double executionTime) {
        this.executionCount++;
        this.totalExecutionTime += executionTime;
        this.averageExecutionTime = this.totalExecutionTime / this.executionCount;
        
        if (this.maxExecutionTime < executionTime) {
            this.maxExecutionTime = executionTime;
        }
        
        if (this.minExecutionTime == 0 || this.minExecutionTime > executionTime) {
            this.minExecutionTime = executionTime;
        }
        
        this.lastExecutionTime = LocalDateTime.now();
        
        if (this.firstExecutionTime == null) {
            this.firstExecutionTime = LocalDateTime.now();
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
}
