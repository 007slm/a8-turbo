package org.openjdbcproxy.grpc.server.smartcache.api.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.List;

/**
 * 规则统计信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleStats {
    
    /**
     * 总规则数量
     */
    private int totalRules;
    
    /**
     * 启用的规则数量
     */
    private int enabledRules;
    
    /**
     * 禁用的规则数量
     */
    private int disabledRules;
    
    /**
     * 按规则类型统计
     */
    private Map<CacheRuleInfo.RuleType, Integer> rulesByType;
    
    /**
     * 按优先级统计
     */
    private Map<Integer, Integer> rulesByPriority;
    
    /**
     * 热门表名统计（被规则引用最多的表）
     */
    private List<TableUsage> topTables;
    
    /**
     * 规则匹配统计
     */
    private RuleMatchStats matchStats;
    
    /**
     * 缓存性能统计
     */
    private CachePerformanceStats performanceStats;
    
    /**
     * 表使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableUsage {
        private String tableName;
        private int ruleCount;
        private int matchCount;
        private double hitRate;
    }
    
    /**
     * 规则匹配统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleMatchStats {
        private long totalMatches;
        private long successfulMatches;
        private long failedMatches;
        private double matchSuccessRate;
        private Map<String, Long> matchesByRule;
    }
    
    /**
     * 缓存性能统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachePerformanceStats {
        private double averageHitRate;
        private long totalCacheHits;
        private long totalCacheMisses;
        private long totalCacheRequests;
        private double averageResponseTime;
        private long totalCacheSize;
        private long expiredEntries;
    }
    
    /**
     * 获取规则启用率
     */
    public double getEnableRate() {
        if (totalRules == 0) {
            return 0.0;
        }
        return (double) enabledRules / totalRules * 100;
    }
    
    /**
     * 获取平均优先级
     */
    public double getAveragePriority() {
        if (rulesByPriority == null || rulesByPriority.isEmpty()) {
            return 0.0;
        }
        
        long totalPriority = 0;
        int totalCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : rulesByPriority.entrySet()) {
            totalPriority += (long) entry.getKey() * entry.getValue();
            totalCount += entry.getValue();
        }
        
        return totalCount > 0 ? (double) totalPriority / totalCount : 0.0;
    }
}
