package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.config.CacheConfig;
import org.openjdbcproxy.cache.entity.CacheRule;

import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.CacheRuleManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 缓存规则管理服务实现类
 * 严格按照设计文档要求：去除分页参数，全量返回，按数据库分组
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CacheRuleManagementServiceImpl implements CacheRuleManagementService {

    private final CacheRuleRepository cacheRuleRepository;
    private final CacheConfig cacheConfig;

    @Override
    @SneakyThrows
    public Map<String, List<CacheRule>> getRules() {
        log.info("获取缓存规则列表（按数据库分组）");
        
        // 获取所有缓存规则
        List<CacheRule> allRules = cacheRuleRepository.findAll();
        
        // 按connHash分组返回规则
        Map<String, List<CacheRule>> groupedRules = allRules.stream()
            .collect(Collectors.groupingBy(rule -> 
                StringUtils.hasText(rule.getConnHash()) ? rule.getConnHash() : "unknown"));
        
        log.info("成功获取缓存规则列表，总数: {}, 数据库数量: {}", allRules.size(), groupedRules.size());
        return groupedRules;
    }



    // 内部方法：根据ID获取规则
    private Optional<CacheRule> getRuleById(String ruleId) {
        if (!StringUtils.hasText(ruleId)) {
            return Optional.empty();
        }
        
        return cacheRuleRepository.findById(ruleId);
    }

    @Override
    @SneakyThrows
    public CacheRule createRule(CacheRule rule) {
        log.info("创建缓存规则: {}", rule.getName());

        CacheRule savedRule = cacheRuleRepository.save(rule);
        log.info("成功创建缓存规则: {}", savedRule.getId());
        
        return savedRule;
    }



    @Override
    @SneakyThrows
    public CacheRule updateRule(String ruleId, CacheRule request) {
        log.info("更新缓存规则: {}", ruleId);
        
        Optional<CacheRule> optionalRule = cacheRuleRepository.findById(ruleId);
        if (!optionalRule.isPresent()) {
            throw new RuntimeException("缓存规则不存在: " + ruleId);
        }
        
        CacheRule rule = optionalRule.get();
        rule.setName(request.getName());
        rule.setConnHash(request.getConnHash());  // 添加connHash字段更新
        rule.setTablesAll(request.getTablesAll());
        rule.setTablesAny(request.getTablesAny());
        rule.setRuleType(request.getRuleType() != null ? request.getRuleType() : rule.getRuleType());
        rule.setTtl(request.getTtl());
        rule.setEnabled(request.isEnabled());
        rule.setDescription(request.getDescription());
        rule.setPriority(request.getPriority());
        rule.setQueryReg(request.getQueryReg());
        rule.setUpdatedAt(LocalDateTime.now());
        
        CacheRule savedRule = cacheRuleRepository.save(rule);
        log.info("成功更新缓存规则: {}", savedRule.getId());
        
        return savedRule;
    }



    @Override
    @SneakyThrows
    public void deleteRule(String ruleId) {
        log.info("删除缓存规则: {}", ruleId);
        
        Optional<CacheRule> optionalRule = cacheRuleRepository.findById(ruleId);
        if (!optionalRule.isPresent()) {
            throw new RuntimeException("缓存规则不存在: " + ruleId);
        }
        
        cacheRuleRepository.deleteById(ruleId);
        log.info("成功删除缓存规则: {}", ruleId);
    }

    @Override
    public List<CacheRule> getCacheRulesByConnHash(String connHash) {
        log.info("获取连接哈希缓存规则: {}", connHash);
        // 由于废弃了datasourceName，返回所有规则
        return cacheRuleRepository.findAll();
    }

    @Override
    public CacheRule matchCacheRule(String connHash, String sql, List<String> tables) {
        log.debug("匹配缓存规则: connHash={}, tables={}", connHash, tables);

        List<CacheRule> rules = getCacheRulesByConnHash(connHash);
        if (CollectionUtils.isEmpty(rules)) {
            return null;
        }
        
        // 按优先级排序，优先级高的先匹配
        return rules.stream()
                .filter(CacheRule::isEnabled)
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .filter(rule -> matchesRule(rule, sql, tables))
                .findFirst()
                .orElse(null);
    }

    /**
     * 启用规则（内部方法）
     */
    public boolean enableRule(String ruleId) {
        return updateRuleStatus(ruleId, true);
    }

    /**
     * 禁用规则（内部方法）
     */
    public boolean disableRule(String ruleId) {
        return updateRuleStatus(ruleId, false);
    }



    /**
     * 检查规则是否匹配
     */
    @SneakyThrows
    private boolean matchesRule(CacheRule rule, String sql, List<String> tables) {
        switch (rule.getRuleType()) {
            case TABLES_ALL:
                // 精确匹配所有表
                return rule.getTablesAll() != null && rule.getTablesAll().containsAll(tables);
            case TABLES_ANY:
                // 匹配任意一个表
                return rule.getTablesAny() != null &&
                       tables.stream().anyMatch(table -> rule.getTablesAny().contains(table));
            case REGEX:
                // SQL条件匹配（简单的包含匹配）
                return sql.toLowerCase().contains(rule.getQueryReg().toLowerCase());
            default:
                return false;
        }
    }

    /**
     * 更新规则启用状态
     */
    @SneakyThrows
    private boolean updateRuleStatus(String ruleId, boolean enabled) {
        if (!StringUtils.hasText(ruleId)) {
            return false;
        }
        
        // 由于废弃了updateEnabled方法，直接通过update方法更新
        Optional<CacheRule> ruleOpt = cacheRuleRepository.findById(ruleId);
        if (ruleOpt.isPresent()) {
            CacheRule rule = ruleOpt.get();
            rule.setEnabled(enabled);
            rule.setUpdatedAt(LocalDateTime.now());
            cacheRuleRepository.update(rule);
            log.info("Cache rule {} {}: {}", enabled ? "enabled" : "disabled", ruleId);
            return true;
        }
        return false;
    }

}