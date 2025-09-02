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
import java.util.UUID;

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
            
            // 按优先级排序规则（优先级高的先匹配）
            rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
            
            // 匹配规则
            for (CacheRule rule : rules) {
                if (rule.matches(sql, tables, request.getParameters())) {
                    return CacheDecision.hit(cacheKey, rule.getTtlDuration(), rule.getName());
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
     * 保存缓存规则到Redis
     */
    public void saveCacheRules(List<CacheRule> rules) {
        try {
            redisTemplate.opsForValue().set(CACHE_RULES_KEY, rules);
            // 更新版本号
            redisTemplate.opsForValue().increment(CACHE_RULES_VERSION_KEY);
            log.info("Saved {} cache rules to Redis", rules.size());
        } catch (Exception e) {
            log.error("Error saving cache rules to Redis", e);
            throw new RuntimeException("Failed to save cache rules", e);
        }
    }
    
    /**
     * 获取规则版本号
     */
    public Long getRulesVersion() {
        try {
            Object version = redisTemplate.opsForValue().get(CACHE_RULES_VERSION_KEY);
            return version instanceof Long ? (Long) version : 0L;
        } catch (Exception e) {
            log.error("Error getting rules version", e);
            return 0L;
        }
    }
    
    /**
     * 从SQL中提取表名
     */
    public List<String> extractTablesFromSql(String sql) {
        List<String> tables = new java.util.ArrayList<>();
        
        if (sql == null || sql.trim().isEmpty()) {
            return tables;
        }
        
        // 转换为大写以便匹配关键字
        String upperSql = sql.toUpperCase();
        
        // 提取FROM子句中的表名
        String[] fromParts = upperSql.split("\\bFROM\\b");
        for (int i = 1; i < fromParts.length; i++) {
            String part = fromParts[i].trim();
            String tableName = extractTableNameFromPart(part);
            if (tableName != null) {
                tables.add(tableName.toLowerCase());
            }
        }
        
        // 提取JOIN子句中的表名
        String[] joinParts = upperSql.split("\\bJOIN\\b");
        for (int i = 1; i < joinParts.length; i++) {
            String part = joinParts[i].trim();
            String tableName = extractTableNameFromPart(part);
            if (tableName != null) {
                tables.add(tableName.toLowerCase());
            }
        }
        
        // 提取UPDATE子句中的表名
        if (upperSql.startsWith("UPDATE")) {
            String[] updateParts = upperSql.split("\\bUPDATE\\b");
            if (updateParts.length > 1) {
                String part = updateParts[1].trim();
                String tableName = extractTableNameFromPart(part);
                if (tableName != null) {
                    tables.add(tableName.toLowerCase());
                }
            }
        }
        
        // 提取DELETE子句中的表名
        if (upperSql.startsWith("DELETE")) {
            String[] deleteParts = upperSql.split("\\bFROM\\b");
            if (deleteParts.length > 1) {
                String part = deleteParts[1].trim();
                String tableName = extractTableNameFromPart(part);
                if (tableName != null) {
                    tables.add(tableName.toLowerCase());
                }
            }
        }
        
        return tables;
    }
    
    /**
     * 从SQL片段中提取表名
     */
    private String extractTableNameFromPart(String part) {
        if (part == null || part.trim().isEmpty()) {
            return null;
        }
        
        // 分割空格，取第一个非空的部分
        String[] words = part.trim().split("\\s+");
        if (words.length == 0) {
            return null;
        }
        
        String tableName = words[0];
        
        // 移除可能的别名（AS关键字后的部分）
        if (tableName.contains(".")) {
            // 处理schema.table格式
            return tableName;
        }
        
        // 移除可能的括号和分号
        tableName = tableName.replaceAll("[;,\\(\\)]", "");
        
        // 检查是否是子查询
        if (tableName.startsWith("(") || tableName.equals("SELECT")) {
            return null;
        }
        
        return tableName.isEmpty() ? null : tableName;
    }
    
    /**
     * 生成唯一的规则ID
     */
    public String generateRuleId() {
        return "rule-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 验证规则是否与现有规则冲突
     */
    public boolean isRuleConflict(CacheRule newRule, List<CacheRule> existingRules) {
        // 检查名称冲突
        boolean nameConflict = existingRules.stream()
                .anyMatch(rule -> rule.getName().equals(newRule.getName()));
        
        if (nameConflict) {
            return true;
        }
        
        // 检查规则逻辑冲突（相同优先级和相同匹配条件）
        return existingRules.stream()
                .anyMatch(rule -> rule.getPriority() == newRule.getPriority() && 
                                rule.getRuleType() == newRule.getRuleType() &&
                                rule.getRuleMatch().equals(newRule.getRuleMatch()));
    }
}
