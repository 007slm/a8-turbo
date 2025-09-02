package org.openjdbcproxy.grpc.server.service;

import org.openjdbcproxy.grpc.server.dto.CacheStatsDto;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.engine.CacheRuleEngine;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule.CacheRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;

/**
 * 缓存规则管理服务
 * 提供缓存规则的CRUD操作和规则验证
 */
@Service
public class CacheRuleService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CacheRuleEngine cacheRuleEngine;

    /**
     * 获取所有缓存规则
     */
    public List<CacheRule> getAllRules() {
        try {
            return cacheRuleEngine.loadCacheRules();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cache rules", e);
        }
    }

    /**
     * 根据ID获取缓存规则
     */
    public Optional<CacheRule> getRuleById(String id) {
        return getAllRules().stream()
                .filter(rule -> id.equals(rule.getId()))
                .findFirst();
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
        
        // 生成规则ID
        rule.setId(cacheRuleEngine.generateRuleId());
        
        // 检查名称是否已存在
        if (rules.stream().anyMatch(r -> rule.getName().equals(r.getName()))) {
            throw new IllegalArgumentException("Rule with name '" + rule.getName() + "' already exists");
        }
        
        // 检查规则冲突
        if (cacheRuleEngine.isRuleConflict(rule, rules)) {
            throw new IllegalArgumentException("Rule conflicts with existing rules");
        }
        
        // 设置默认值
        if (rule.getStatus() == null) {
            rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        }
        if (rule.getLastUpdated() == null) {
            rule.setLastUpdated(LocalDateTime.now());
        }
        if (rule.getPriority() == 0) {
            rule.setPriority(5); // 默认优先级
        }
        
        rules.add(rule);
        cacheRuleEngine.saveCacheRules(rules);
        
        return rule;
    }

    /**
     * 更新缓存规则
     */
    public CacheRule updateRule(String id, CacheRule updatedRule) {
        validateRule(updatedRule);
        
        List<CacheRule> rules = getAllRules();
        boolean found = false;
        
        for (int i = 0; i < rules.size(); i++) {
            if (id.equals(rules.get(i).getId())) {
                updatedRule.setId(id); // 保持原有ID
                updatedRule.setLastUpdated(LocalDateTime.now());
                rules.set(i, updatedRule);
                found = true;
                break;
            }
        }
        
        if (!found) {
            throw new IllegalArgumentException("Rule with id '" + id + "' not found");
        }
        
        cacheRuleEngine.saveCacheRules(rules);
        return updatedRule;
    }

    /**
     * 删除缓存规则
     */
    public boolean deleteRule(String id) {
        List<CacheRule> rules = getAllRules();
        boolean removed = rules.removeIf(rule -> id.equals(rule.getId()));
        
        if (removed) {
            cacheRuleEngine.saveCacheRules(rules);
        }
        
        return removed;
    }

    /**
     * 启用/禁用缓存规则
     */
    public CacheRule toggleRule(String id, boolean enabled) {
        List<CacheRule> rules = getAllRules();
        
        for (CacheRule rule : rules) {
            if (id.equals(rule.getId())) {
                rule.setEnabled(enabled);
                rule.setStatus(enabled ? CacheRule.RuleStatus.ACTIVE : CacheRule.RuleStatus.INACTIVE);
                rule.setLastUpdated(LocalDateTime.now());
                cacheRuleEngine.saveCacheRules(rules);
                return rule;
            }
        }
        
        throw new IllegalArgumentException("Rule with id '" + id + "' not found");
    }

    /**
     * 创建表格精确匹配规则
     */
    public CacheRule createTablesRule(String name, String description, List<String> tables, String ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(CacheRule.RuleType.TABLES);
        rule.setTables(tables);
        rule.setRuleMatch(String.join(",", tables));
        rule.setTtl(ttl);
        rule.setPriority(10);
        rule.setEnabled(true);
        rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        
        return createRule(rule);
    }

    /**
     * 创建表格任意匹配规则
     */
    public CacheRule createTablesAnyRule(String name, String description, List<String> tables, String ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(CacheRule.RuleType.TABLES_ANY);
        rule.setTablesAny(tables);
        rule.setRuleMatch(String.join(",", tables));
        rule.setTtl(ttl);
        rule.setPriority(8);
        rule.setEnabled(true);
        rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        
        return createRule(rule);
    }

    /**
     * 创建表格全部匹配规则
     */
    public CacheRule createTablesAllRule(String name, String description, List<String> tables, String ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(CacheRule.RuleType.TABLES_ALL);
        rule.setTablesAll(tables);
        rule.setRuleMatch(String.join(",", tables));
        rule.setTtl(ttl);
        rule.setPriority(6);
        rule.setEnabled(true);
        rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        
        return createRule(rule);
    }

    /**
     * 创建查询ID匹配规则
     */
    public CacheRule createQueryIdsRule(String name, String description, List<String> queryIds, String ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(CacheRule.RuleType.QUERY_IDS);
        rule.setQueryIds(queryIds);
        rule.setRuleMatch(String.join(",", queryIds));
        rule.setTtl(ttl);
        rule.setPriority(12); // 最高优先级
        rule.setEnabled(true);
        rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        
        return createRule(rule);
    }

    /**
     * 创建正则表达式规则
     */
    public CacheRule createRegexRule(String name, String description, String regex, String ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(CacheRule.RuleType.REGEX);
        rule.setRegex(regex);
        rule.setRuleMatch(regex);
        rule.setTtl(ttl);
        rule.setPriority(4);
        rule.setEnabled(true);
        rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        
        return createRule(rule);
    }

    /**
     * 创建全局规则
     */
    public CacheRule createAnyRule(String name, String description, String ttl) {
        CacheRule rule = new CacheRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(CacheRule.RuleType.ANY);
        rule.setRuleMatch("匹配所有");
        rule.setTtl(ttl);
        rule.setPriority(0); // 最低优先级
        rule.setEnabled(true);
        rule.setStatus(CacheRule.RuleStatus.ACTIVE);
        
        return createRule(rule);
    }

    /**
     * 验证缓存规则
     */
    public CacheStatsDto.RuleValidationResult validateRule(CacheRule rule) {
        CacheStatsDto.RuleValidationResult result = new CacheStatsDto.RuleValidationResult();
        result.setValid(true);
        result.setMessage("规则验证通过");
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        
        try {
            // 基本验证
            if (rule.getName() == null || rule.getName().trim().isEmpty()) {
                result.getErrors().add("规则名称不能为空");
                result.setValid(false);
            }
            
            if (rule.getRuleType() == null) {
                result.getErrors().add("规则类型不能为空");
                result.setValid(false);
            }
            
            if (rule.getTtl() == null || rule.getTtl().trim().isEmpty()) {
                result.getErrors().add("TTL不能为空");
                result.setValid(false);
            } else {
                try {
                    rule.getTtlDuration(); // 验证TTL格式
                } catch (Exception e) {
                    result.getErrors().add("TTL格式无效: " + e.getMessage());
                    result.setValid(false);
                }
            }
            
            // 根据规则类型验证特定字段
            switch (rule.getRuleType()) {
                case TABLES:
                    if (rule.getTables() == null || rule.getTables().isEmpty()) {
                        result.getErrors().add("表格精确匹配规则必须指定表格列表");
                        result.setValid(false);
                    }
                    break;
                case TABLES_ANY:
                    if (rule.getTablesAny() == null || rule.getTablesAny().isEmpty()) {
                        result.getErrors().add("表格任意匹配规则必须指定表格列表");
                        result.setValid(false);
                    }
                    break;
                case TABLES_ALL:
                    if (rule.getTablesAll() == null || rule.getTablesAll().isEmpty()) {
                        result.getErrors().add("表格全部匹配规则必须指定表格列表");
                        result.setValid(false);
                    }
                    break;
                case QUERY_IDS:
                    if (rule.getQueryIds() == null || rule.getQueryIds().isEmpty()) {
                        result.getErrors().add("查询ID匹配规则必须指定查询ID列表");
                        result.setValid(false);
                    }
                    break;
                case REGEX:
                    if (rule.getRegex() == null || rule.getRegex().trim().isEmpty()) {
                        result.getErrors().add("正则表达式规则必须指定正则表达式");
                        result.setValid(false);
                    } else {
                        try {
                            java.util.regex.Pattern.compile(rule.getRegex());
                        } catch (Exception e) {
                            result.getErrors().add("无效的正则表达式: " + e.getMessage());
                            result.setValid(false);
                        }
                    }
                    break;
                case ANY:
                    // 全局规则不需要额外验证
                    break;
            }
            
            // 检查规则冲突
            List<CacheRule> existingRules = getAllRules();
            if (cacheRuleEngine.isRuleConflict(rule, existingRules)) {
                result.getWarnings().add("规则可能与现有规则冲突");
            }
            
        } catch (Exception e) {
            result.getErrors().add("规则验证过程中发生错误: " + e.getMessage());
            result.setValid(false);
        }
        
        if (!result.isValid()) {
            result.setMessage("规则验证失败: " + String.join(", ", result.getErrors()));
        }
        
        return result;
    }

    /**
     * 验证所有规则
     */
    public CacheStatsDto.RuleValidationResult validateAllRules() {
        CacheStatsDto.RuleValidationResult result = new CacheStatsDto.RuleValidationResult();
        result.setValid(true);
        result.setMessage("所有规则验证通过");
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        
        List<CacheRule> rules = getAllRules();
        
        for (CacheRule rule : rules) {
            CacheStatsDto.RuleValidationResult ruleResult = validateRule(rule);
            if (!ruleResult.isValid()) {
                result.setValid(false);
                result.getErrors().add("规则 '" + rule.getName() + "': " + ruleResult.getMessage());
            }
            result.getWarnings().addAll(ruleResult.getWarnings());
        }
        
        if (!result.isValid()) {
            result.setMessage("部分规则验证失败");
        }
        
        return result;
    }

    /**
     * 获取规则版本号
     */
    public Long getRulesVersion() {
        return cacheRuleEngine.getRulesVersion();
    }

    /**
     * 清空所有规则
     */
    public void clearAllRules() {
        try {
            cacheRuleEngine.saveCacheRules(List.of());
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear cache rules", e);
        }
    }

    /**
     * 获取活跃规则数量
     */
    public long getActiveRulesCount() {
        return getAllRules().stream()
                .filter(rule -> rule.getStatus() == CacheRule.RuleStatus.ACTIVE)
                .count();
    }

    /**
     * 获取规则统计信息
     */
    public Map<String, Long> getRuleTypeStats() {
        return getAllRules().stream()
                .collect(Collectors.groupingBy(
                    rule -> rule.getRuleType().getValue(),
                    Collectors.counting()
                ));
    }

    /**
     * 根据查询ID获取相关规则
     */
    public List<CacheRule> getRulesByQueryId(String queryId) {
        return getAllRules().stream()
                .filter(rule -> {
                    switch (rule.getRuleType()) {
                        case QUERY_IDS:
                            return rule.getQueryIds() != null && 
                                   rule.getQueryIds().contains(queryId);
                        case ANY:
                            return true; // 全局规则匹配所有查询
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据表格名称获取相关规则
     */
    public List<CacheRule> getRulesByTableName(String tableName) {
        return getAllRules().stream()
                .filter(rule -> {
                    switch (rule.getRuleType()) {
                        case TABLES:
                        case TABLES_ANY:
                        case TABLES_ALL:
                            return rule.getTables() != null && 
                                   rule.getTables().contains(tableName);
                        case ANY:
                            return true; // 全局规则匹配所有表格
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
