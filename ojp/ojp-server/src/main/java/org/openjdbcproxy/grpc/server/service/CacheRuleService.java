package org.openjdbcproxy.grpc.server.service;

import org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule.CacheRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 缓存规则管理服务
 * 提供缓存规则的CRUD操作和规则验证
 */
@Service
public class CacheRuleService {

    private static final String CACHE_RULES_KEY = "ojp:cache:rules";
    private static final String CACHE_RULES_VERSION_KEY = "ojp:cache:rules:version";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取所有缓存规则
     */
    public List<CacheRule> getAllRules() {
        try {
            @SuppressWarnings("unchecked")
            List<CacheRule> rules = (List<CacheRule>) redisTemplate.opsForValue().get(CACHE_RULES_KEY);
            return rules != null ? rules : List.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cache rules", e);
        }
    }

    /**
     * 根据名称获取缓存规则
     */
    public Optional<CacheRule> getRuleByName(String name) {
        return getAllRules().stream()
                .filter(rule -> name.equals(rule.getName()))
                .findFirst();
    }

    /**
     * 创建新的缓存规则
     */
    public CacheRule createRule(CacheRule rule) {
        validateRule(rule);
        
        List<CacheRule> rules = getAllRules();
        
        // 检查名称是否已存在
        if (rules.stream().anyMatch(r -> rule.getName().equals(r.getName()))) {
            throw new IllegalArgumentException("Rule with name '" + rule.getName() + "' already exists");
        }
        
        rules.add(rule);
        saveRules(rules);
        
        return rule;
    }

    /**
     * 更新缓存规则
     */
    public CacheRule updateRule(String name, CacheRule updatedRule) {
        validateRule(updatedRule);
        
        List<CacheRule> rules = getAllRules();
        boolean found = false;
        
        for (int i = 0; i < rules.size(); i++) {
            if (name.equals(rules.get(i).getName())) {
                rules.set(i, updatedRule);
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new IllegalArgumentException("Rule with name '" + name + "' not found");
        }
        
        saveRules(rules);
        return updatedRule;
    }

    /**
     * 删除缓存规则
     */
    public boolean deleteRule(String name) {
        List<CacheRule> rules = getAllRules();
        boolean removed = rules.removeIf(rule -> name.equals(rule.getName()));
        
        if (removed) {
            saveRules(rules);
        }
        
        return removed;
    }

    /**
     * 启用/禁用缓存规则
     */
    public CacheRule toggleRule(String name, boolean enabled) {
        List<CacheRule> rules = getAllRules();
        
        for (CacheRule rule : rules) {
            if (name.equals(rule.getName())) {
                rule.setEnabled(enabled);
                saveRules(rules);
                return rule;
            }
        }
        
        throw new IllegalArgumentException("Rule with name '" + name + "' not found");
    }

    /**
     * 创建表名规则
     */
    public CacheRule createTableRule(String name, String description, List<String> tables, Duration ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setType(CacheRule.RuleType.TABLE_NAME);
        rule.setTables(tables);
        rule.setTtl(ttl);
        rule.setPriority(10);
        rule.setEnabled(true);
        
        return createRule(rule);
    }

    /**
     * 创建正则表达式规则
     */
    public CacheRule createRegexRule(String name, String description, String regex, Duration ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setType(CacheRule.RuleType.REGEX);
        rule.setRegex(regex);
        rule.setTtl(ttl);
        rule.setPriority(5);
        rule.setEnabled(true);
        
        return createRule(rule);
    }

    /**
     * 创建查询类型规则
     */
    public CacheRule createQueryTypeRule(String name, String description, String queryType, Duration ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setType(CacheRule.RuleType.QUERY_TYPE);
        rule.setPattern(queryType);
        rule.setTtl(ttl);
        rule.setPriority(1);
        rule.setEnabled(true);
        
        return createRule(rule);
    }

    /**
     * 创建全局规则
     */
    public CacheRule createGlobalRule(String name, String description, Duration ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setType(CacheRule.RuleType.GLOBAL);
        rule.setTtl(ttl);
        rule.setPriority(0);
        rule.setEnabled(true);
        
        return createRule(rule);
    }

    /**
     * 保存规则到Redis
     */
    private void saveRules(List<CacheRule> rules) {
        try {
            redisTemplate.opsForValue().set(CACHE_RULES_KEY, rules);
            // 更新版本号
            redisTemplate.opsForValue().increment(CACHE_RULES_VERSION_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save cache rules", e);
        }
    }

    /**
     * 验证缓存规则
     */
    private void validateRule(CacheRule rule) {
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }
        
        if (rule.getType() == null) {
            throw new IllegalArgumentException("Rule type is required");
        }
        
        if (rule.getTtl() == null || rule.getTtl().isNegative() || rule.getTtl().isZero()) {
            throw new IllegalArgumentException("Rule TTL must be positive");
        }
        
        // 根据规则类型验证特定字段
        switch (rule.getType()) {
            case TABLE_NAME:
                if ((rule.getTables() == null || rule.getTables().isEmpty()) &&
                    (rule.getTablesAny() == null || rule.getTablesAny().isEmpty()) &&
                    (rule.getTablesAll() == null || rule.getTablesAll().isEmpty())) {
                    throw new IllegalArgumentException("Table name rule must specify tables, tablesAny, or tablesAll");
                }
                break;
            case REGEX:
                if (rule.getRegex() == null || rule.getRegex().trim().isEmpty()) {
                    throw new IllegalArgumentException("Regex rule must specify regex pattern");
                }
                // 验证正则表达式是否有效
                try {
                    java.util.regex.Pattern.compile(rule.getRegex());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
                }
                break;
            case QUERY_TYPE:
                if (rule.getPattern() == null || rule.getPattern().trim().isEmpty()) {
                    throw new IllegalArgumentException("Query type rule must specify pattern");
                }
                break;
            case GLOBAL:
                // 全局规则不需要额外验证
                break;
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
            return 0L;
        }
    }

    /**
     * 清空所有规则
     */
    public void clearAllRules() {
        try {
            redisTemplate.delete(CACHE_RULES_KEY);
            redisTemplate.delete(CACHE_RULES_VERSION_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear cache rules", e);
        }
    }
}
