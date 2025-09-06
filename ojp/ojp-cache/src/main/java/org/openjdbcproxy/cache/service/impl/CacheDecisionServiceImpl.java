package org.openjdbcproxy.cache.service.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.config.CacheConfig;
import org.openjdbcproxy.cache.entity.CacheDecision;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.CacheDecisionService;
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
public class CacheDecisionServiceImpl implements CacheDecisionService {

    private final CacheRuleRepository cacheRuleRepository;
    private final CacheConfig cacheConfig;
    private final SqlParseUtil sqlParseUtil;

    public CacheDecisionServiceImpl(CacheRuleRepository cacheRuleRepository,
                                  CacheConfig cacheConfig,
                                  SqlParseUtil sqlParseUtil) {
        this.cacheRuleRepository = cacheRuleRepository;
        this.cacheConfig = cacheConfig;
        this.sqlParseUtil = sqlParseUtil;
    }

    @Override
    @SneakyThrows
    public CacheDecision makeDecision(String datasourceName, String sql) {
        // 解析SQL生成Query对象
        Query query = SqlParseUtil.parseQuery(sql, datasourceName);
        return makeDecision(datasourceName, query);
    }

    @Override
    public CacheDecision makeDecision(String datasourceName, Query query) {
        if (!cacheConfig.isEnabled()) {
            return CacheDecision.noCache(query.getQueryId(), datasourceName, 
                "Cache is disabled", CacheDecision.DecisionType.DEFAULT_NO_CACHE);
        }

        // 检查SQL类型，只缓存SELECT语句
        if (!query.isSelectQuery()) {
            return CacheDecision.noCache(query.getQueryId(), datasourceName, 
                "Only SELECT queries can be cached", CacheDecision.DecisionType.UNSUPPORTED_QUERY_TYPE);
        }

        // 检查查询涉及的表
        List<String> tableNames = query.getTables();
        if (CollectionUtils.isEmpty(tableNames)) {
            return CacheDecision.noCache(query.getQueryId(), datasourceName, 
                "No tables found in query", CacheDecision.DecisionType.TABLE_NOT_IN_RULES);
        }

        // 查找匹配的缓存规则
        List<CacheRule> rules = cacheRuleRepository.findEnabledRulesOrderByPriority(
            datasourceName);
        
        CacheRule matchedRule = findMatchingRule(query, rules);
        if (matchedRule == null) {
            return CacheDecision.noCache(query.getQueryId(), datasourceName, 
                "No matching cache rule found", CacheDecision.DecisionType.TABLE_NOT_IN_RULES);
        }

        // 计算TTL
        long ttl = calculateTtl(matchedRule);
        String cacheKey = query.getCacheKey();

        log.debug("Cache decision: USE_CACHE for query {} with TTL {} seconds", 
            query.getQueryId(), ttl);

        return CacheDecision.useCache(query.getQueryId(), datasourceName, 
            cacheKey, (int) ttl, matchedRule.getId(), 
            "Matched rule: " + matchedRule.getName());
    }

    @Override
    public boolean hasTableCacheRule(String dataSourceName, String tableName) {
        if (!StringUtils.hasText(tableName) || !StringUtils.hasText(dataSourceName)) {
            return false;
        }

        List<CacheRule> rules = cacheRuleRepository.findEnabledRulesOrderByPriority(
            dataSourceName);
        
        return rules.stream()
            .anyMatch(rule -> rule.getTables() != null && rule.getTables().contains(tableName));
    }

    @Override
    public int getQueryTtl(String datasourceName, Query query) {
        List<CacheRule> rules = cacheRuleRepository.findEnabledRulesOrderByPriority(
            datasourceName);
        
        CacheRule matchedRule = findMatchingRule(query, rules);
        if (matchedRule != null) {
            return (int) calculateTtl(matchedRule);
        }
        
        return (int) cacheConfig.getDefaultTtl();
    }

    @Override
    @SneakyThrows
    public void refreshCacheRules(String dataSourceName) {
        log.info("Refreshing cache rules for data source: {}", dataSourceName);
        
        // 重新加载规则（如果有缓存的话）
        if (cacheConfig.isRuleCacheEnabled()) {
            cacheRuleRepository.reloadRules(dataSourceName);
        }
        
        log.info("Cache rules refreshed successfully for data source: {}", dataSourceName);
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