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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuerySummary {

    /**
     * 查询ID
     */
    private String queryId;

    /**
     * SQL语句（可能被截断）
     */
    private String sql;

    /**
     * 涉及的表名列表
     */
    private List<String> tables;

    /**
     * 访问次数
     */
    private Long accessCount;

    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;

    /**
     * 缓存命中率
     */
    private Double cacheHitRate;

    /**
     * 是否有缓存
     */
    private Boolean hasCache;

    /**
     * 缓存TTL（秒）
     */
    private Integer cacheTtl;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 查询类型
     */
    private String queryType;

    /**
     * 缓存状态
     */
    private CacheStatus cacheStatus;

    /**
     * 缓存状态枚举
     */
    public enum CacheStatus {
        /**
         * 已缓存
         */
        CACHED,
        
        /**
         * 未缓存
         */
        NOT_CACHED,
        
        /**
         * 缓存已过期
         */
        EXPIRED,
        
        /**
         * 未知状态
         */
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

    /**
     * 获取截断的SQL（用于显示）
     */
    public String getTruncatedSql(int maxLength) {
        if (sql == null) {
            return "";
        }
        if (sql.length() <= maxLength) {
            return sql;
        }
        return sql.substring(0, maxLength - 3) + "...";
    }
}