package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存概览统计数据传输对象
 * 提供系统级别的缓存性能概览信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheOverviewStats {
    
    /**
     * 总规则数量
     */
    private Integer totalRules;
    
    /**
     * 活跃规则数量
     */
    private Integer activeRules;
    
    /**
     * 总查询数量
     */
    private Long totalQueries;
    
    /**
     * 缓存查询数量
     */
    private Long cachedQueries;
    
    /**
     * 总体命中率
     */
    private Double overallHitRate;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;
    
    /**
     * 总缓存大小（字节）
     */
    private Long totalCacheSize;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
}