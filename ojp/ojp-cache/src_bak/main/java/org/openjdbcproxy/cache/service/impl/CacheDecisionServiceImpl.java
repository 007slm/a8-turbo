package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.config.CacheConfig;
import org.openjdbcproxy.cache.entity.CacheDecision;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.AsyncStatsService;
import org.openjdbcproxy.cache.service.CacheDecisionService;
import org.openjdbcproxy.cache.service.CacheRuleEngine;
import org.openjdbcproxy.cache.util.JSqlParserUtil;
import org.openjdbcproxy.cache.util.SqlParseUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 缓存决策服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheDecisionServiceImpl implements CacheDecisionService {

    private final CacheRuleRepository cacheRuleRepository;
    private final CacheConfig cacheConfig;
    private final SqlParseUtil sqlParseUtil;
    private final CacheRuleEngine cacheRuleEngine;
    private final AsyncStatsService asyncStatsService;

    @Override
    @SneakyThrows
    public CacheDecision makeDecision(String connHash, String sql) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 解析SQL生成Query对象
            Query query = JSqlParserUtil.parseQuery(sql, connHash);
            if (query == null) {
                log.warn("SQL解析失败，跳过缓存: {}", sql);
                
                // 异步记录解析失败统计
                Query failedQuery = new Query();
                failedQuery.setQueryType(Query.QueryType.UNKNOWN);
                asyncStatsService.recordQueryStats(connHash, failedQuery, false, System.currentTimeMillis() - startTime);
                
                return CacheDecision.noCache(null, connHash, "SQL解析失败", 
                    CacheDecision.DecisionType.UNSUPPORTED_QUERY_TYPE);
            }
            
            CacheDecision decision = makeDecision(connHash, query);
            
            // 记录决策统计
            long decisionTime = System.currentTimeMillis() - startTime;
            asyncStatsService.recordQueryStats(connHash, query, decision.isUseCache(), decisionTime);
            
            // 记录表级别统计
            if (query.getTables() != null && !query.getTables().isEmpty()) {
                for (String tableName : query.getTables()) {
                    asyncStatsService.recordTableAccess(connHash, tableName, 
                        decision.isUseCache(), decisionTime);
                }
            }
            
            log.debug("缓存决策完成: sql={}, shouldCache={}, reason={}, ttl={}, decisionTime={}ms", 
                    sql, decision.isUseCache(), decision.getReason(), decision.getTtlSeconds(), decisionTime);
            
            return decision;
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            log.error("缓存决策失败: sql={}, errorTime={}ms", sql, errorTime, e);
            
            // 异步记录错误统计
            Query errorQuery = new Query();
            errorQuery.setQueryType(Query.QueryType.UNKNOWN);
            asyncStatsService.recordQueryStats(connHash, errorQuery, false, errorTime);
            
            return CacheDecision.noCache(null, connHash, 
                "决策过程异常: " + e.getMessage(), CacheDecision.DecisionType.DEFAULT_NO_CACHE);
        }
    }

    @Override
    public boolean hasTableCacheRule(String connHash, String tableName) {
        return false;
    }

    @Override
    public CacheDecision makeDecision(String connHash, Query query) {
        if (!cacheConfig.getCacheRule().isCacheEnabled()) {
            return CacheDecision.noCache(query.getQueryId(), connHash, 
                "Cache is disabled", CacheDecision.DecisionType.DEFAULT_NO_CACHE);
        }

        // 使用CacheRuleEngine进行决策
        return cacheRuleEngine.makeDecision(connHash, query);
    }


    @Override
    public int getQueryTtl(String connHash, Query query) {
        return cacheRuleEngine.getQueryTtl(connHash, query);
    }

    @Override
    public void refreshCacheRules(String connHash) {
        cacheRuleEngine.refreshCacheRules(connHash);
        log.info("Cache rules refreshed successfully for connHash: {}", connHash);
    }

    @Override
    public List<String> extractTables(String sql) {
        return cacheRuleEngine.extractTablesFromSql(sql);
    }

    /**
     * 查找匹配的缓存规则
     */
    private CacheRule findMatchingRule(Query query, List<CacheRule> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return null;
        }

        // 按优先级排序，优先级高的先匹配
        return rules.stream()
            .filter(rule -> rule.matches(query))
            .min((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
            .orElse(null);
    }

    /**
     * 计算有效的TTL值
     */
    private long calculateTtl(CacheRule rule) {
        long ttl = rule.getTtl();
        
        // 应用配置的TTL限制
        if (ttl > cacheConfig.getMaxTtl()) {
            ttl = cacheConfig.getMaxTtl();
        } else if (ttl < cacheConfig.getMinTtl()) {
            ttl = cacheConfig.getMinTtl();
        }
        
        return ttl;
    }
}