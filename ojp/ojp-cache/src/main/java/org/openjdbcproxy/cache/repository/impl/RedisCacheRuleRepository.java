package org.openjdbcproxy.cache.repository.impl;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于Redis的缓存规则存储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisCacheRuleRepository implements CacheRuleRepository {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis键前缀
    private static final String RULE_PREFIX = "fluxdb:cache_rule:";
    private static final String RULE_LIST_PREFIX = "fluxdb:cache_rule_list:";
    private static final String RULE_NAME_PREFIX = "fluxdb:cache_rule_name:";
    private static final String RULE_TABLE_PREFIX = "fluxdb:cache_rule_table:";
    private static final String RULE_ENABLED_PREFIX = "fluxdb:cache_rule_enabled:";

    // 默认过期时间（30天）
    private static final long DEFAULT_EXPIRE_DAYS = 30;

    @Override
    public CacheRule save(CacheRule rule) {
        try {
            if (rule.getId() == null) {
                rule.setId(generateRuleId());
            }
            
            if (rule.getCreatedAt() == null) {
                rule.setCreatedAt(LocalDateTime.now());
            }
            rule.setUpdatedAt(LocalDateTime.now());
            
            String ruleKey = RULE_PREFIX + rule.getId();
            String listKey = RULE_LIST_PREFIX + (rule.getDatasourceName() != null ? rule.getDatasourceName() : "global");
            String nameKey = RULE_NAME_PREFIX + (rule.getDatasourceName() != null ? rule.getDatasourceName() : "global") + ":" + rule.getName();
            
            // 保存规则详情
            String ruleJson = JSON.toJSONString(rule);
            redisTemplate.opsForValue().set(ruleKey, ruleJson, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            
            // 添加到规则列表（按优先级排序）
            redisTemplate.opsForZSet().add(listKey, rule.getId(), rule.getPriority());
            
            // 保存名称索引
            redisTemplate.opsForValue().set(nameKey, rule.getId(), DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            
            // 保存表关联索引
            saveTableAssociations(rule);
            
            // 保存启用状态索引
            if (rule.isEnabled()) {
                String enabledKey = RULE_ENABLED_PREFIX + (rule.getDatasourceName() != null ? rule.getDatasourceName() : "global");
                redisTemplate.opsForZSet().add(enabledKey, rule.getId(), rule.getPriority());
            }
            
            log.debug("Saved cache rule: {}", rule.getId());
            return rule;
        } catch (Exception e) {
            log.error("Failed to save cache rule: {}", rule.getId(), e);
            throw new RuntimeException("Failed to save cache rule", e);
        }
    }

    @Override
    public Optional<CacheRule> findById(String ruleId) {
        try {
            String ruleKey = RULE_PREFIX + ruleId;
            String ruleJson = redisTemplate.opsForValue().get(ruleKey);
            
            if (ruleJson == null) {
                return Optional.empty();
            }
            
            CacheRule rule = JSON.parseObject(ruleJson, CacheRule.class);
            return Optional.of(rule);
        } catch (Exception e) {
            log.error("Failed to find cache rule by id: {}", ruleId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<CacheRule> findAll() {
        try {
            Set<String> datasources = getDatasources();
            List<CacheRule> allRules = new ArrayList<>();
            
            for (String datasource : datasources) {
                List<CacheRule> rules = findByDatasource(datasource.equals("global") ? null : datasource);
                allRules.addAll(rules);
            }
            
            // 按优先级排序
            allRules.sort(Comparator.comparingInt(CacheRule::getPriority));
            return allRules;
        } catch (Exception e) {
            log.error("Failed to find all cache rules", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CacheRule> findByDatasource(String datasourceName) {
        try {
            String listKey = RULE_LIST_PREFIX + (datasourceName != null ? datasourceName : "global");
            Set<String> ruleIds = redisTemplate.opsForZSet().range(listKey, 0, -1);
            
            if (ruleIds == null || ruleIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<CacheRule> rules = new ArrayList<>();
            for (String ruleId : ruleIds) {
                Optional<CacheRule> ruleOpt = findById(ruleId);
                if (ruleOpt.isPresent()) {
                    rules.add(ruleOpt.get());
                }
            }
            
            return rules;
        } catch (Exception e) {
            log.error("Failed to find cache rules by datasource: {}", datasourceName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CacheRule> findEnabledRulesOrderByPriority(String datasourceName) {
        try {
            String enabledKey = RULE_ENABLED_PREFIX + (datasourceName != null ? datasourceName : "global");
            Set<String> ruleIds = redisTemplate.opsForZSet().range(enabledKey, 0, -1);
            
            if (ruleIds == null || ruleIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<CacheRule> rules = new ArrayList<>();
            for (String ruleId : ruleIds) {
                Optional<CacheRule> ruleOpt = findById(ruleId);
                if (ruleOpt.isPresent() && ruleOpt.get().isEnabled()) {
                    rules.add(ruleOpt.get());
                }
            }
            
            return rules;
        } catch (Exception e) {
            log.error("Failed to find enabled cache rules by datasource: {}", datasourceName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CacheRule> findByTable(String tableName, String datasourceName) {
        try {
            String tableKey = RULE_TABLE_PREFIX + (datasourceName != null ? datasourceName : "global") + ":" + tableName;
            Set<String> ruleIds = redisTemplate.opsForSet().members(tableKey);
            
            if (ruleIds == null || ruleIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<CacheRule> rules = new ArrayList<>();
            for (String ruleId : ruleIds) {
                Optional<CacheRule> ruleOpt = findById(ruleId);
                if (ruleOpt.isPresent()) {
                    rules.add(ruleOpt.get());
                }
            }
            
            // 按优先级排序
            rules.sort(Comparator.comparingInt(CacheRule::getPriority));
            return rules;
        } catch (Exception e) {
            log.error("Failed to find cache rules by table: {} for datasource: {}", tableName, datasourceName, e);
            return Collections.emptyList();
        }
    }

    @Override
    public CacheRule update(CacheRule rule) {
        try {
            // 先删除旧的索引
            Optional<CacheRule> oldRuleOpt = findById(rule.getId());
            if (oldRuleOpt.isPresent()) {
                removeIndexes(oldRuleOpt.get());
            }
            
            // 保存更新后的规则
            return save(rule);
        } catch (Exception e) {
            log.error("Failed to update cache rule: {}", rule.getId(), e);
            throw new RuntimeException("Failed to update cache rule", e);
        }
    }

    @Override
    public boolean deleteById(String ruleId) {
        try {
            Optional<CacheRule> ruleOpt = findById(ruleId);
            if (!ruleOpt.isPresent()) {
                return false;
            }
            
            CacheRule rule = ruleOpt.get();
            
            // 删除规则详情
            String ruleKey = RULE_PREFIX + ruleId;
            redisTemplate.delete(ruleKey);
            
            // 删除所有索引
            removeIndexes(rule);
            
            log.debug("Deleted cache rule: {}", ruleId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete cache rule: {}", ruleId, e);
            return false;
        }
    }

    @Override
    public int deleteByDatasource(String datasourceName) {
        try {
            List<CacheRule> rules = findByDatasource(datasourceName);
            int deletedCount = 0;
            
            for (CacheRule rule : rules) {
                if (deleteById(rule.getId())) {
                    deletedCount++;
                }
            }
            
            log.info("Deleted {} cache rules for datasource: {}", deletedCount, datasourceName);
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to delete cache rules by datasource: {}", datasourceName, e);
            return 0;
        }
    }

    @Override
    public boolean updateEnabled(String ruleId, boolean enabled) {
        try {
            Optional<CacheRule> ruleOpt = findById(ruleId);
            if (!ruleOpt.isPresent()) {
                return false;
            }
            
            CacheRule rule = ruleOpt.get();
            rule.setEnabled(enabled);
            rule.setUpdatedAt(LocalDateTime.now());
            
            save(rule);
            return true;
        } catch (Exception e) {
            log.error("Failed to update enabled status for rule: {}", ruleId, e);
            return false;
        }
    }

    @Override
    public int batchUpdateEnabled(List<String> ruleIds, boolean enabled) {
        int updatedCount = 0;
        for (String ruleId : ruleIds) {
            if (updateEnabled(ruleId, enabled)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }

    @Override
    public boolean updatePriority(String ruleId, int priority) {
        try {
            Optional<CacheRule> ruleOpt = findById(ruleId);
            if (!ruleOpt.isPresent()) {
                return false;
            }
            
            CacheRule rule = ruleOpt.get();
            rule.setPriority(priority);
            rule.setUpdatedAt(LocalDateTime.now());
            
            save(rule);
            return true;
        } catch (Exception e) {
            log.error("Failed to update priority for rule: {}", ruleId, e);
            return false;
        }
    }

    @Override
    public int batchUpdatePriorities(Map<String, Integer> rulePriorities) {
        int updatedCount = 0;
        for (Map.Entry<String, Integer> entry : rulePriorities.entrySet()) {
            if (updatePriority(entry.getKey(), entry.getValue())) {
                updatedCount++;
            }
        }
        return updatedCount;
    }

    @Override
    public long count() {
        try {
            Set<String> datasources = getDatasources();
            long totalCount = 0;
            
            for (String datasource : datasources) {
                totalCount += countByDatasource(datasource.equals("global") ? null : datasource);
            }
            
            return totalCount;
        } catch (Exception e) {
            log.error("Failed to count cache rules", e);
            return 0;
        }
    }

    @Override
    public long countByDatasource(String datasourceName) {
        try {
            String listKey = RULE_LIST_PREFIX + (datasourceName != null ? datasourceName : "global");
            Long count = redisTemplate.opsForZSet().count(listKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to count cache rules by datasource: {}", datasourceName, e);
            return 0;
        }
    }

    @Override
    public long countEnabled(String datasourceName) {
        try {
            String enabledKey = RULE_ENABLED_PREFIX + (datasourceName != null ? datasourceName : "global");
            Long count = redisTemplate.opsForZSet().count(enabledKey, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to count enabled cache rules by datasource: {}", datasourceName, e);
            return 0;
        }
    }

    @Override
    public boolean existsByName(String name, String datasourceName, String excludeRuleId) {
        try {
            String nameKey = RULE_NAME_PREFIX + (datasourceName != null ? datasourceName : "global") + ":" + name;
            String existingRuleId = redisTemplate.opsForValue().get(nameKey);
            
            if (existingRuleId == null) {
                return false;
            }
            
            return !existingRuleId.equals(excludeRuleId);
        } catch (Exception e) {
            log.error("Failed to check if rule name exists: {}", name, e);
            return false;
        }
    }

    @Override
    public int batchSave(List<CacheRule> rules) {
        int savedCount = 0;
        for (CacheRule rule : rules) {
            try {
                save(rule);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save cache rule in batch: {}", rule.getId(), e);
            }
        }
        return savedCount;
    }

    @Override
    public Optional<CacheRule> findDefaultRule(String datasourceName) {
        try {
            List<CacheRule> rules = findEnabledRulesOrderByPriority(datasourceName);
            
            // 查找默认规则（优先级最低或规则类型为DEFAULT）
            return rules.stream()
                    .filter(rule -> rule.getRuleType() == CacheRule.RuleType.DEFAULT)
                    .findFirst()
                    .or(() -> rules.stream().max(Comparator.comparingInt(CacheRule::getPriority)));
        } catch (Exception e) {
            log.error("Failed to find default rule for datasource: {}", datasourceName, e);
            return Optional.empty();
        }
    }

    @Override
    public List<CacheRule> reloadRules(String datasourceName) {
        try {
            // 清除缓存（如果有的话）
            log.info("Reloading cache rules for datasource: {}", datasourceName);
            return findEnabledRulesOrderByPriority(datasourceName);
        } catch (Exception e) {
            log.error("Failed to reload rules for datasource: {}", datasourceName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成规则ID
     */
    private String generateRuleId() {
        return "rule_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取所有数据源名称
     */
    private Set<String> getDatasources() {
        try {
            Set<String> keys = redisTemplate.keys(RULE_LIST_PREFIX + "*");
            if (keys == null) {
                return Collections.emptySet();
            }
            
            return keys.stream()
                    .map(key -> key.substring(RULE_LIST_PREFIX.length()))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to get datasources", e);
            return Collections.emptySet();
        }
    }

    /**
     * 保存表关联索引
     */
    private void saveTableAssociations(CacheRule rule) {
        String datasourceKey = rule.getDatasourceName() != null ? rule.getDatasourceName() : "global";
        
        // 保存tables关联
        if (rule.getTables() != null) {
            for (String table : rule.getTables()) {
                String tableKey = RULE_TABLE_PREFIX + datasourceKey + ":" + table;
                redisTemplate.opsForSet().add(tableKey, rule.getId());
                redisTemplate.expire(tableKey, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            }
        }
        
        // 保存tablesAny关联
        if (rule.getTablesAny() != null) {
            for (String table : rule.getTablesAny()) {
                String tableKey = RULE_TABLE_PREFIX + datasourceKey + ":" + table;
                redisTemplate.opsForSet().add(tableKey, rule.getId());
                redisTemplate.expire(tableKey, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
            }
        }
    }

    /**
     * 删除所有索引
     */
    private void removeIndexes(CacheRule rule) {
        String datasourceKey = rule.getDatasourceName() != null ? rule.getDatasourceName() : "global";
        String listKey = RULE_LIST_PREFIX + datasourceKey;
        String nameKey = RULE_NAME_PREFIX + datasourceKey + ":" + rule.getName();
        String enabledKey = RULE_ENABLED_PREFIX + datasourceKey;
        
        // 从规则列表中移除
        redisTemplate.opsForZSet().remove(listKey, rule.getId());
        
        // 删除名称索引
        redisTemplate.delete(nameKey);
        
        // 从启用列表中移除
        redisTemplate.opsForZSet().remove(enabledKey, rule.getId());
        
        // 从表关联索引中移除
        if (rule.getTables() != null) {
            for (String table : rule.getTables()) {
                String tableKey = RULE_TABLE_PREFIX + datasourceKey + ":" + table;
                redisTemplate.opsForSet().remove(tableKey, rule.getId());
            }
        }
        
        if (rule.getTablesAny() != null) {
            for (String table : rule.getTablesAny()) {
                String tableKey = RULE_TABLE_PREFIX + datasourceKey + ":" + table;
                redisTemplate.opsForSet().remove(tableKey, rule.getId());
            }
        }
    }
}