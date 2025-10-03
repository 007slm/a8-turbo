package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存概览统计数据传输对象
 * 提供系统级别的缓存性能概览信息
 *
 * 字段说明:
 * totalRules: 总规则数量
 * activeRules: 活跃规则数量
 * totalQueries: 总查询数量
 * cachedQueries: 缓存查询数量
 * overallHitRate: 总体命中率
 * avgResponseTime: 平均响应时间（毫秒）
 * totalCacheSize: 总缓存大小（字节）
 * lastUpdated: 最后更新时间
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheOverviewStats {
    
    private Integer totalRules;
    private Integer activeRules;
    private Long totalQueries;
    private Long cachedQueries;
    private Double overallHitRate;
    private Double avgResponseTime;
    private Long totalCacheSize;
    private LocalDateTime lastUpdated;
}