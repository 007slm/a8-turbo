package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 查询摘要信息
 * 用于查询列表展示
 *
 * 字段说明:
 * queryId: 查询ID
 * sql: SQL语句（可能被截断）
 * tables: 涉及的表名列表
 * accessCount: 访问次数
 * avgResponseTime: 平均响应时间（毫秒）
 * cacheHitRate: 缓存命中率
 * hasCache: 是否有缓存
 * cacheTtl: 缓存TTL（秒）
 * lastAccessTime: 最后访问时间
 * connHash: 连接哈希值
 * queryType: 查询类型
 * cacheStatus: 缓存状态
 *
 * CacheStatus枚举说明:
 * CACHED: 已缓存
 * NOT_CACHED: 未缓存
 * EXPIRED: 缓存已过期
 * UNKNOWN: 未知状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuerySummary {

    private String queryId;
    private String sql;
    private List<String> tables;
    private Long accessCount;
    private Double avgResponseTime = 0.0d;
    private Double cacheHitRate = 0.0d;
    private Boolean hasCache;
    private Integer cacheTtl;
    private LocalDateTime lastAccessTime;
    private String connHash;
    private String queryType;
    private CacheStatus cacheStatus;

    /**
     * 缓存状态枚举
     */
    public enum CacheStatus {
        /** 已缓存 */
        CACHED,
        
        /** 未缓存 */
        NOT_CACHED,
        
        /** 缓存已过期 */
        EXPIRED,
        
        /** 未知状态 */
        UNKNOWN
    }

    /**
     * 获取格式化的缓存命中率
     */
    public String getFormattedCacheHitRate() {
        if (cacheHitRate == null) {
            return "N/A";
        }
        return String.format("%.1f%%", cacheHitRate * 100);
    }

    /**
     * 获取格式化的平均响应时间
     */
    public String getFormattedAvgResponseTime() {
        if (avgResponseTime == null) {
            return "N/A";
        }
        return String.format("%.2fms", avgResponseTime);
    }

}