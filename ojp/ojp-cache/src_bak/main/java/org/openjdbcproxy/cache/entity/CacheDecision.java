package org.openjdbcproxy.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 缓存决策结果实体类
 *
 * 字段说明:
 * useCache: 是否使用缓存
 * ttlSeconds: 缓存TTL（生存时间，秒）
 * cacheKey: 缓存键
 * matchedRuleId: 匹配的缓存规则ID
 * ruleName: 匹配的缓存规则名称
 * skipReason: 跳过缓存的原因
 * reason: 决策原因
 * decisionTime: 决策时间
 * queryId: 查询ID
 * connHash: 连接哈希
 * decisionType: 决策类型
 *
 * DecisionType枚举说明:
 * RULE_BASED: 基于规则的决策
 * DEFAULT_NO_CACHE: 默认决策（不缓存）
 * UNSUPPORTED_QUERY_TYPE: 查询类型不支持缓存
 * TABLE_NOT_IN_RULES: 表不在缓存规则中
 * RULE_DISABLED: 规则被禁用
 * ZERO_TTL: TTL为0（明确不缓存）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDecision {

    private boolean useCache;
    private int ttlSeconds;
    private String cacheKey;
    private String matchedRuleId;
    private String ruleName;
    private String skipReason;
    private String reason;
    private LocalDateTime decisionTime;
    private String queryId;
    private String connHash;
    private DecisionType decisionType;

    public enum DecisionType {
        RULE_BASED,
        DEFAULT_NO_CACHE,
        UNSUPPORTED_QUERY_TYPE,
        TABLE_NOT_IN_RULES,
        RULE_DISABLED,
        ZERO_TTL
    }

    /**
     * 创建不使用缓存的决策
     */
    public static CacheDecision noCache(String queryId, String connHash, String reason, DecisionType type) {
        return CacheDecision.builder()
                .useCache(false)
                .ttlSeconds(0)
                .queryId(queryId)
                .connHash(connHash)
                .reason(reason)
                .decisionType(type)
                .decisionTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建使用缓存的决策
     */
    public static CacheDecision useCache(String queryId, String connHash, String cacheKey, 
                                       int ttlSeconds, String matchedRuleId, String reason) {
        return CacheDecision.builder()
                .useCache(true)
                .ttlSeconds(ttlSeconds)
                .cacheKey(cacheKey)
                .queryId(queryId)
                .connHash(connHash)
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