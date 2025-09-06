package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.config.CacheConfig;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.dto.CacheRuleResponse;
import org.openjdbcproxy.cache.dto.CreateCacheRuleRequest;
import org.openjdbcproxy.cache.dto.UpdateCacheRuleRequest;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.CacheRuleManagementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    @SneakyThrows
    public Map<String, List<CacheRuleResponse>> getRules() {
        log.info("获取缓存规则列表（按数据库分组）");
        
        // 获取所有缓存规则
        List<CacheRule> allRules = cacheRuleRepository.findAll();
        
        // 按数据库名称分组并转换为响应DTO
        Map<String, List<CacheRuleResponse>> groupedRules = allRules.stream()
            .collect(Collectors.groupingBy(
                CacheRule::getDatasourceName,
                Collectors.mapping(this::convertToResponse, Collectors.toList())
            ));
        
        log.info("成功获取缓存规则列表，总数: {}, 数据库数量: {}", allRules.size(), groupedRules.size());
        return groupedRules;
    }

    /**
     * 转换CacheRule为CacheRuleResponse
     */
    private CacheRuleResponse convertToResponse(CacheRule rule) {
        return CacheRuleResponse.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .datasourceName(rule.getDatasourceName())
            .dbType("MySQL") // 默认数据库类型，可根据实际情况调整
            .tables(rule.getTables())
            .ruleType(rule.getRuleType() != null ? rule.getRuleType().name().toLowerCase() : "tables")
            .ttl(rule.getTtl())
            .enabled(rule.isEnabled())
            .description(rule.getDescription())
            .priority(rule.getPriority())
            .createdAt(rule.getCreatedAt() != null ? rule.getCreatedAt().format(ISO_FORMATTER) : null)
            .updatedAt(rule.getUpdatedAt() != null ? rule.getUpdatedAt().format(ISO_FORMATTER) : null)
            .matchedQueries(List.of()) // 暂时返回空列表，后续可根据需要实现
            .build();
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
    public CacheRuleResponse createRule(CreateCacheRuleRequest request) {
        log.info("创建缓存规则: {}", request.getRuleName());
        
        CacheRule rule = CacheRule.builder()
            .id(UUID.randomUUID().toString())
            .name(request.getRuleName())
            .datasourceName(request.getDatasourceName())
            .tables(request.getTables())
            .ruleType(parseRuleType(request.getRuleType()))
            .ttl(request.getTtl())
            .enabled(request.isEnabled())
            .description(request.getDescription())
            .priority(request.getPriority())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        CacheRule savedRule = cacheRuleRepository.save(rule);
        log.info("成功创建缓存规则: {}", savedRule.getId());
        
        return convertToResponse(savedRule);
    }

    /**
     * 解析规则类型
     */
    @SneakyThrows
    private CacheRule.RuleType parseRuleType(String ruleType) {
        if (ruleType == null) {
            return CacheRule.RuleType.TABLE_MATCH;
        }
        switch (ruleType.toLowerCase()) {
            case "tables":
                return CacheRule.RuleType.TABLE_MATCH;
            case "tablesany":
                return CacheRule.RuleType.TABLE_ANY_MATCH;
            case "condition":
                return CacheRule.RuleType.CONDITION_MATCH;
            default:
                return CacheRule.RuleType.TABLE_MATCH;
        }
    }

    @Override
    @SneakyThrows
    public CacheRuleResponse updateRule(String ruleId, UpdateCacheRuleRequest request) {
        log.info("更新缓存规则: {}", ruleId);
        
        Optional<CacheRule> optionalRule = cacheRuleRepository.findById(ruleId);
        if (!optionalRule.isPresent()) {
            throw new RuntimeException("缓存规则不存在: " + ruleId);
        }
        
        CacheRule rule = optionalRule.get();
        rule.setName(request.getRuleName());
        rule.setTables(request.getTables());
        rule.setRuleType(parseRuleType(request.getRuleType()));
        rule.setTtl(request.getTtl());
        rule.setEnabled(request.isEnabled());
        rule.setDescription(request.getDescription());
        rule.setPriority(request.getPriority());
        rule.setUpdatedAt(LocalDateTime.now());
        
        CacheRule savedRule = cacheRuleRepository.save(rule);
        log.info("成功更新缓存规则: {}", savedRule.getId());
        
        return convertToResponse(savedRule);
    }

    /**
     * 获取缓存规则详情（内部方法）
     */
    @SneakyThrows
    private CacheRuleResponse getRuleDetail(String ruleId) {
        log.info("获取缓存规则详情: {}", ruleId);
        
        Optional<CacheRule> optionalRule = cacheRuleRepository.findById(ruleId);
        if (!optionalRule.isPresent()) {
            throw new RuntimeException("缓存规则不存在: " + ruleId);
        }
        
        CacheRule rule = optionalRule.get();
        log.info("成功获取缓存规则详情: {}", rule.getId());
        
        return convertToResponse(rule);
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
    @SneakyThrows
    public List<CacheRule> getCacheRulesByDatasource(String datasourceName) {
        log.info("获取数据源缓存规则: {}", datasourceName);
        return cacheRuleRepository.findByDatasource(datasourceName);
    }

    @Override
    @SneakyThrows
    public CacheRule matchCacheRule(String datasourceName, String sql, List<String> tables) {
        log.debug("匹配缓存规则: datasource={}, tables={}", datasourceName, tables);
        
        List<CacheRule> rules = getCacheRulesByDatasource(datasourceName);
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
            case TABLE_MATCH:
                // 精确匹配所有表
                return rule.getTables() != null && rule.getTables().containsAll(tables);
            case TABLE_ANY_MATCH:
                // 匹配任意一个表
                return rule.getTables() != null && 
                       tables.stream().anyMatch(table -> rule.getTables().contains(table));
            case CONDITION_MATCH:
                // SQL条件匹配（简单的包含匹配）
                return sql.toLowerCase().contains(rule.getCondition().toLowerCase());
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
        
        boolean updated = cacheRuleRepository.updateEnabled(ruleId, enabled);
        if (updated) {
            log.info("Cache rule {} {}: {}", enabled ? "enabled" : "disabled", ruleId);
        }
        return updated;
    }

}