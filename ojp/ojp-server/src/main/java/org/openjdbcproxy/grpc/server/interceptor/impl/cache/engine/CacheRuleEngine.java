package org.openjdbcproxy.grpc.server.interceptor.impl.cache.engine;

import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.decision.CacheDecision;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule.CacheRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 缓存规则引擎
 * 负责从Redis加载缓存规则，进行规则匹配和缓存决策
 */
@Slf4j
@Component
public class CacheRuleEngine {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_RULES_KEY = "ojp:cache:rules";
    private static final String CACHE_RULES_VERSION_KEY = "ojp:cache:rules:version";
    
    /**
     * 判断查询是否应该缓存
     */
    public CacheDecision shouldCacheQuery(StatementRequest request, SessionInfo session, String cacheKey) {
        try {
            // 从Redis获取缓存规则
            List<CacheRule> rules = loadCacheRules();
            if (rules.isEmpty()) {
                return CacheDecision.skip("No cache rules configured");
            }

            String sql = request.getSql().trim();
            List<String> tables = extractTablesFromSql(sql);
            
            // 按优先级排序规则
            rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
            
            // 匹配规则
            for (CacheRule rule : rules) {
                if (rule.matches(sql, tables, request.getParameters())) {
                    return CacheDecision.miss(cacheKey, rule.getTtl(), rule.getName());
                }
            }
            
            return CacheDecision.skip("No matching cache rule");
            
        } catch (Exception e) {
            log.error("Error evaluating cache rules", e);
            return CacheDecision.skip("Error evaluating cache rules: " + e.getMessage());
        }
    }
    
    /**
     * 从Redis加载缓存规则
     */
    @SuppressWarnings("unchecked")
    public List<CacheRule> loadCacheRules() {
        try {
            Object rulesObj = redisTemplate.opsForValue().get(CACHE_RULES_KEY);
            if (rulesObj instanceof List) {
                return (List<CacheRule>) rulesObj;
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error loading cache rules from Redis", e);
            return List.of();
        }
    }
    
    /**
     * 从SQL中提取表名
     */
    public List<String> extractTablesFromSql(String sql) {
        // 简单的表名提取逻辑，实际项目中可以使用SQL解析器
        List<String> tables = new java.util.ArrayList<>();
        
        // 提取FROM和JOIN后的表名
        String upperSql = sql.toUpperCase();
        String[] parts = upperSql.split("\\s+");
        
        for (int i = 0; i < parts.length - 1; i++) {
            if ("FROM".equals(parts[i]) || "JOIN".equals(parts[i])) {
                if (i + 1 < parts.length) {
                    String tableName = parts[i + 1].replaceAll("[;,\\(\\)]", "");
                    if (!tableName.isEmpty() && !tableName.startsWith("(")) {
                        tables.add(tableName.toLowerCase());
                    }
                }
            }
        }
        
        return tables;
    }
}
