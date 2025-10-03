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
 * 
 * 字段说明:
 * queryId: 查询ID
 * sql: 完整SQL语句
 * normalizedSql: 规范化SQL语句
 * tables: 涉及的表名列表
 * queryType: 查询类型
 * connHash: 连接哈希值
 * createdAt: 创建时间
 * lastAccessTime: 最后访问时间
 * accessCount: 访问次数
 * cacheHitCount: 缓存命中次数
 * cacheHitRate: 缓存命中率
 * avgResponseTime: 平均响应时间（毫秒）
 * minResponseTime: 最小响应时间（毫秒）
 * maxResponseTime: 最大响应时间（毫秒）
 * cacheInfo: 缓存信息
 * matchedRules: 匹配的缓存规则
 * performanceStats: 性能统计
 * attributes: 扩展属性
 * 
 * CacheInfo内部类字段说明:
 * hasCache: 是否有缓存
 * cacheKey: 缓存键
 * ttl: 缓存TTL（秒）
 * cacheSize: 缓存大小（字节）
 * cacheCreatedAt: 缓存创建时间
 * cacheExpiresAt: 缓存过期时间
 * cacheStatus: 缓存状态
 * 
 * MatchedRule内部类字段说明:
 * ruleId: 规则ID
 * ruleName: 规则名称
 * description: 规则描述
 * ttl: TTL（秒）
 * priority: 优先级
 * matched: 是否匹配
 * matchReason: 匹配原因
 * 
 * PerformanceStats内部类字段说明:
 * totalExecutionTime: 总执行时间（毫秒）
 * cacheSavedTime: 缓存节省的时间（毫秒）
 * performanceImprovement: 性能提升比例
 * last24HourAccess: 最近24小时访问次数
 * last7DayAccess: 最近7天访问次数
 * last30DayAccess: 最近30天访问次数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryDetailResponse {
    private String queryId;
    private String sql;
    private String normalizedSql;
    private List<String> tables;
    private String queryType;
    private String connHash;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessTime;
    private Long accessCount;
    private Long cacheHitCount;
    private Double cacheHitRate;
    private Double avgResponseTime;
    private Long minResponseTime;
    private Long maxResponseTime;
    private CacheInfo cacheInfo;
    private List<MatchedRule> matchedRules;
    private PerformanceStats performanceStats;
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