package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 查询详情响应
 * 用于查询详情页面展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryDetailResponse {

    /**
     * 查询ID
     */
    private String queryId;

    /**
     * 完整SQL语句
     */
    private String sql;

    /**
     * 规范化SQL语句
     */
    private String normalizedSql;

    /**
     * 涉及的表名列表
     */
    private List<String> tables;

    /**
     * 查询类型
     */
    private String queryType;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 访问次数
     */
    private Long accessCount;

    /**
     * 缓存命中次数
     */
    private Long cacheHitCount;

    /**
     * 缓存命中率
     */
    private Double cacheHitRate;

    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;

    /**
     * 最小响应时间（毫秒）
     */
    private Long minResponseTime;

    /**
     * 最大响应时间（毫秒）
     */
    private Long maxResponseTime;

    /**
     * 缓存信息
     */
    private CacheInfo cacheInfo;

    /**
     * 匹配的缓存规则
     */
    private List<MatchedRule> matchedRules;

    /**
     * 性能统计
     */
    private PerformanceStats performanceStats;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;

    /**
     * 缓存信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheInfo {
        /**
         * 是否有缓存
         */
        private Boolean hasCache;
        
        /**
         * 缓存键
         */
        private String cacheKey;
        
        /**
         * 缓存TTL（秒）
         */
        private Integer ttl;
        
        /**
         * 缓存大小（字节）
         */
        private Long cacheSize;
        
        /**
         * 缓存创建时间
         */
        private LocalDateTime cacheCreatedAt;
        
        /**
         * 缓存过期时间
         */
        private LocalDateTime cacheExpiresAt;
        
        /**
         * 缓存状态
         */
        private String cacheStatus;
    }

    /**
     * 匹配的规则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedRule {
        /**
         * 规则ID
         */
        private String ruleId;
        
        /**
         * 规则名称
         */
        private String ruleName;
        
        /**
         * 规则描述
         */
        private String description;
        
        /**
         * TTL（秒）
         */
        private Integer ttl;
        
        /**
         * 优先级
         */
        private Integer priority;
        
        /**
         * 是否匹配
         */
        private Boolean matched;
        
        /**
         * 匹配原因
         */
        private String matchReason;
    }

    /**
     * 性能统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceStats {
        /**
         * 总执行时间（毫秒）
         */
        private Long totalExecutionTime;
        
        /**
         * 缓存节省的时间（毫秒）
         */
        private Long cacheSavedTime;
        
        /**
         * 性能提升比例
         */
        private Double performanceImprovement;
        
        /**
         * 最近24小时访问次数
         */
        private Long last24HourAccess;
        
        /**
         * 最近7天访问次数
         */
        private Long last7DayAccess;
        
        /**
         * 最近30天访问次数
         */
        private Long last30DayAccess;
    }

    /**
     * 获取格式化的缓存命中率
     */
    public String getFormattedCacheHitRate() {
        if (cacheHitRate == null) {
            return "N/A";
        }
        return String.format("%.2f%%", cacheHitRate * 100);
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