package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheDecision;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.entity.RuleType;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.util.JSqlParserUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存规则引擎
 * 负责规则匹配、缓存决策和规则管理
 * 整合了ojp-server中的CacheRuleEngine和ojp-cache中的CacheDecisionService设计
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheRuleEngine {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheRuleRepository cacheRuleRepository;
    
    private static final String CACHE_RULES_KEY = "ojp:cache:rules";
    private static final String CACHE_RULES_VERSION_KEY = "ojp:cache:rules:version";
    
    // 规则缓存，按数据源名称分组
    private final ConcurrentHashMap<String, List<CacheRule>> rulesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> rulesCacheVersion = new ConcurrentHashMap<>();
    
    /**
     * 核心决策方法
     * 根据查询和数据源名称做出缓存决策
     * 增强版：支持复杂规则匹配和智能优先级排序，优先使用connHash
     */
    public CacheDecision makeDecision(String connHash, Query query) {
        try {
            // 检查SQL类型，只缓存SELECT语句
            if (!query.isSelectQuery()) {
                return CacheDecision.noCache(query.getQueryId(), connHash, 
                    "Only SELECT queries can be cached", CacheDecision.DecisionType.UNSUPPORTED_QUERY_TYPE);
            }
            
            // 检查查询涉及的表
            List<String> tableNames = query.getTables();
            if (CollectionUtils.isEmpty(tableNames)) {
                return CacheDecision.noCache(query.getQueryId(), connHash, 
                    "No tables found in query", CacheDecision.DecisionType.TABLE_NOT_IN_RULES);
            }
            
            // 获取缓存规则
            List<CacheRule> rules = getCacheRulesByConnHash(connHash);
            log.debug("Using rules for connHash/datasource: {}", connHash);
            
            if (CollectionUtils.isEmpty(rules)) {
                return CacheDecision.noCache(query.getQueryId(), connHash, 
                    "No cache rules configured", CacheDecision.DecisionType.TABLE_NOT_IN_RULES);
            }
            
            // 增强版规则匹配：按匹配度和优先级排序
            List<RuleMatch> matches = evaluateRuleMatches(rules, query);
            
            if (matches.isEmpty()) {
                return CacheDecision.noCache(query.getQueryId(), connHash, 
                    "No matching cache rule found", CacheDecision.DecisionType.TABLE_NOT_IN_RULES);
            }
            
            // 选择最佳匹配规则
            RuleMatch bestMatch = matches.get(0);
            CacheRule rule = bestMatch.getRule();
            
            // 计算TTL
            long ttl = calculateTtl(rule);
            if (ttl <= 0) {
                return CacheDecision.noCache(query.getQueryId(), connHash, 
                    "Rule TTL is zero or negative", CacheDecision.DecisionType.ZERO_TTL);
            }
            
            log.debug("Cache rule matched: {} (score: {}) for query: {}", 
                rule.getName(), bestMatch.getScore(), query.getQueryId());
            return CacheDecision.useCache(query.getQueryId(), connHash, 
                query.getCacheKey(), (int) ttl, rule.getId(), 
                String.format("Matched rule: %s (score: %.2f)", rule.getName(), bestMatch.getScore()));
            
        } catch (Exception e) {
            log.error("Error evaluating cache rules for connHash/datasource: {}", connHash, e);
            return CacheDecision.noCache(query.getQueryId(), connHash, 
                "Error evaluating cache rules: " + e.getMessage(), CacheDecision.DecisionType.DEFAULT_NO_CACHE);
        }
    }
    

    public List<CacheRule> getCacheRules(String connHash) {
        return getCacheRulesByConnHash(connHash);
    }

    /**
     * 根据connHash获取缓存规则（新方法）
     */
    public List<CacheRule> getCacheRulesByConnHash(String connHash) {
        try {
            // 检查版本是否需要更新
            Long currentVersion = getRulesVersion(connHash);
            Long cachedVersion = rulesCacheVersion.get(connHash);
            
            if (cachedVersion == null || !cachedVersion.equals(currentVersion)) {
                // 从数据库重新加载规则
                List<CacheRule> rules = cacheRuleRepository.findEnabledRulesByConnHashOrderByPriority(connHash);
                rulesCache.put(connHash, rules);
                rulesCacheVersion.put(connHash, currentVersion);
                log.debug("Loaded {} cache rules for connHash: {}", rules.size(), connHash);
                return rules;
            }
            
            // 返回缓存的规则
            return rulesCache.getOrDefault(connHash, List.of());
            
        } catch (Exception e) {
            log.error("Error loading cache rules for connHash: {}", connHash, e);
            return List.of();
        }
    }
    
    /**
     * 刷新缓存规则
     */
    public void refreshCacheRules(String connHash) {
        try {
            // 清除缓存
            rulesCache.remove(connHash);
            rulesCacheVersion.remove(connHash);
            
            // 重新加载
            getCacheRulesByConnHash(connHash);
            
            log.info("Cache rules refreshed for connHash: {}", connHash);
        } catch (Exception e) {
            log.error("Error refreshing cache rules for connHash: {}", connHash, e);
        }
    }
    
    /**
     * 保存缓存规则到Redis
     */
    public void saveCacheRules(String connHash, List<CacheRule> rules) {
        try {
            String key = CACHE_RULES_KEY + ":" + connHash;
            redisTemplate.opsForValue().set(key, rules, 1, TimeUnit.HOURS);
            
            // 更新版本号
            String versionKey = CACHE_RULES_VERSION_KEY + ":" + connHash;
            redisTemplate.opsForValue().increment(versionKey);
            
            // 清除本地缓存
            rulesCache.remove(connHash);
            rulesCacheVersion.remove(connHash);
            
            log.info("Saved {} cache rules to Redis for connHash: {}", rules.size(), connHash);
        } catch (Exception e) {
            log.error("Error saving cache rules to Redis for connHash: {}", connHash, e);
            throw new RuntimeException("Failed to save cache rules", e);
        }
    }
    
    /**
     * 获取规则版本号
     */
    public Long getRulesVersion(String connHash) {
        try {
            String versionKey = CACHE_RULES_VERSION_KEY + ":" + connHash;
            Object version = redisTemplate.opsForValue().get(versionKey);
            return version instanceof Long ? (Long) version : 0L;
        } catch (Exception e) {
            log.error("Error getting rules version for connHash: {}", connHash, e);
            return 0L;
        }
    }
    
    /**
     * 计算TTL
     */
    private long calculateTtl(CacheRule rule) {
        if (rule.getTtl() > 0) {
            return rule.getTtl();
        }
        
        // 默认TTL（5分钟）
        return 300;
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
                                hasSameMatchCondition(rule, newRule));
    }
    
    /**
     * 检查两个规则是否有相同的匹配条件
     */
    private boolean hasSameMatchCondition(CacheRule rule1, CacheRule rule2) {
        // 检查表匹配条件
        if (rule1.getRuleType() == RuleType.TABLES_ALL ||
            rule1.getRuleType() == RuleType.TABLES_ANY) {
            return java.util.Objects.equals(rule1.getTablesAll(), rule2.getTablesAll()) &&
                   java.util.Objects.equals(rule1.getTablesAny(), rule2.getTablesAny());
        }
        
        // 检查条件匹配
        if (rule1.getRuleType() == RuleType.REGEX) {
            return java.util.Objects.equals(rule1.getQueryReg(), rule2.getQueryReg());
        }
        
        return false;
    }
    
    /**
     * 获取查询的TTL
     */
    public int getQueryTtl(String connHash, Query query) {
        List<CacheRule> rules = getCacheRules(connHash);
        
        // 使用增强版规则匹配
        List<RuleMatch> matches = evaluateRuleMatches(rules, query);
        
        if (!matches.isEmpty()) {
            CacheRule rule = matches.get(0).getRule();
            return rule.getTtl() > 0 ? rule.getTtl() : 300;
        }
        
        return 0; // 不缓存
    }
    
    /**
      * 规则匹配结果类
      */
     public static class RuleMatch {
         private final CacheRule rule;
         private final double score;
         private final String reason;
         
         public RuleMatch(CacheRule rule, double score, String reason) {
             this.rule = rule;
             this.score = score;
             this.reason = reason;
         }
         
         public CacheRule getRule() { return rule; }
         public double getScore() { return score; }
         public String getReason() { return reason; }
     }
    
    /**
     * 增强版规则匹配评估
     * 计算每个规则的匹配度分数，并按分数排序
     */
    private List<RuleMatch> evaluateRuleMatches(List<CacheRule> rules, Query query) {
        List<RuleMatch> matches = new ArrayList<>();
        
        for (CacheRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            
            double score = calculateRuleMatchScore(rule, query);
            if (score > 0) {
                String reason = buildMatchReason(rule, query, score);
                matches.add(new RuleMatch(rule, score, reason));
            }
        }
        
        // 按分数降序排序（分数高的优先）
        matches.sort((m1, m2) -> Double.compare(m2.getScore(), m1.getScore()));
        
        return matches;
    }
    
    /**
     * 计算规则匹配分数
     * 综合考虑优先级、表匹配度、条件匹配度等因素
     */
    private double calculateRuleMatchScore(CacheRule rule, Query query) {
        if (!rule.matches(query)) {
            return 0.0;
        }
        
        double score = 0.0;
        
        // 基础分数：优先级权重（0-100）
        score += rule.getPriority() * 1.0;
        
        // 表匹配度加分（0-50）
        score += calculateTableMatchScore(rule, query) * 50.0;
        
        // 条件匹配度加分（0-30）
        score += calculateConditionMatchScore(rule, query) * 30.0;
        
        // 规则类型加分（0-20）
        score += calculateRuleTypeScore(rule) * 20.0;
        
        return score;
    }
    
    /**
     * 计算表匹配度分数（0.0-1.0）
     */
    private double calculateTableMatchScore(CacheRule rule, Query query) {
        List<String> queryTables = query.getTables();
        if (CollectionUtils.isEmpty(queryTables)) {
            return 0.0;
        }
        
        Set<String> ruleTables = new HashSet<>();
        if (rule.getTablesAll() != null) {
            ruleTables.addAll(rule.getTablesAll());
        }
        if (rule.getTablesAny() != null) {
            ruleTables.addAll(rule.getTablesAny());
        }
        
        if (ruleTables.isEmpty()) {
            return 1.0; // 通配规则
        }
        
        // 计算交集比例
        long matchCount = queryTables.stream()
            .mapToLong(table -> ruleTables.contains(table) ? 1 : 0)
            .sum();
            
        return (double) matchCount / queryTables.size();
    }
    
    /**
     * 计算条件匹配度分数（0.0-1.0）
     */
    private double calculateConditionMatchScore(CacheRule rule, Query query) {
        if (!StringUtils.hasText(rule.getQueryReg())) {
            return 1.0; // 无条件限制
        }
        
        // 简单的条件匹配评估
        String sql = query.getSql().toLowerCase();
        String condition = rule.getQueryReg().toLowerCase();
        
        if (sql.contains(condition)) {
            return 1.0;
        }
        
        // 可以添加更复杂的条件匹配逻辑
        return 0.5; // 部分匹配
    }
    
    /**
     * 计算规则类型分数（0.0-1.0）
     */
    private double calculateRuleTypeScore(CacheRule rule) {
        switch (rule.getRuleType()) {
            case TABLES_ALL:
                return 1.0; // 精确表匹配最高分
            case TABLES_ANY:
                return 0.8; // 任意表匹配次之
            case REGEX:
                return 0.6; // 正则匹配再次之
            default:
                return 0.4; // 其他类型
        }
    }
    
    /**
     * 构建匹配原因说明
     */
    private String buildMatchReason(CacheRule rule, Query query, double score) {
        StringBuilder reason = new StringBuilder();
        reason.append("Rule '").append(rule.getName()).append("' matched with score ").append(String.format("%.2f", score));
        reason.append(" (priority: ").append(rule.getPriority());
        reason.append(", type: ").append(rule.getRuleType());
        reason.append(", tables: ").append(rule.getTablesAll());

        
        reason.append(")");
        return reason.toString();
    }
}