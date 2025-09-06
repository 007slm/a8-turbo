package org.openjdbcproxy.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存决策结果
 * 封装缓存决策的相关信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDecision {

    /**
     * 是否使用缓存
     */
    private boolean useCache;

    /**
     * 缓存TTL（生存时间，秒）
     */
    private int ttlSeconds;

    /**
     * 缓存键
     */
    private String cacheKey;

    /**
     * 匹配的缓存规则ID
     */
    private String matchedRuleId;

    /**
     * 决策原因
     */
    private String reason;

    /**
     * 决策时间
     */
    private LocalDateTime decisionTime;

    /**
     * 查询ID
     */
    private String queryId;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 决策类型
     */
    private DecisionType decisionType;

    /**
     * 决策类型枚举
     */
    public enum DecisionType {
        /**
         * 基于规则的决策
         */
        RULE_BASED,
        
        /**
         * 默认决策（不缓存）
         */
        DEFAULT_NO_CACHE,
        
        /**
         * 查询类型不支持缓存
         */
        UNSUPPORTED_QUERY_TYPE,
        
        /**
         * 表不在缓存规则中
         */
        TABLE_NOT_IN_RULES,
        
        /**
         * 规则被禁用
         */
        RULE_DISABLED,
        
        /**
         * TTL为0（明确不缓存）
         */
        ZERO_TTL
    }

    /**
     * 创建不使用缓存的决策
     */
    public static CacheDecision noCache(String queryId, String datasourceName, String reason, DecisionType type) {
        return CacheDecision.builder()
                .useCache(false)
                .ttlSeconds(0)
                .queryId(queryId)
                .datasourceName(datasourceName)
                .reason(reason)
                .decisionType(type)
                .decisionTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建使用缓存的决策
     */
    public static CacheDecision useCache(String queryId, String datasourceName, String cacheKey, 
                                       int ttlSeconds, String matchedRuleId, String reason) {
        return CacheDecision.builder()
                .useCache(true)
                .ttlSeconds(ttlSeconds)
                .cacheKey(cacheKey)
                .queryId(queryId)
                .datasourceName(datasourceName)
                .matchedRuleId(matchedRuleId)
                .reason(reason)
                .decisionType(DecisionType.RULE_BASED)
                .decisionTime(LocalDateTime.now())
                .build();
    }

    /**
     * 获取决策摘要信息
     */
    public String getSummary() {
        if (useCache) {
            return String.format("USE_CACHE(ttl=%ds, rule=%s)", ttlSeconds, matchedRuleId);
        } else {
            return String.format("NO_CACHE(%s)", decisionType);
        }
    }

    /**
     * 是否为有效的缓存决策
     */
    public boolean isValidCacheDecision() {
        return useCache && ttlSeconds > 0 && cacheKey != null && !cacheKey.isEmpty();
    }
}