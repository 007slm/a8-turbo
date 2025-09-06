package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.CacheRuleResponse;
import org.openjdbcproxy.cache.dto.CreateTableRuleRequest;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.TableRuleService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 表格缓存规则服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableRuleServiceImpl implements TableRuleService {

    private final CacheRuleRepository cacheRuleRepository;

    @Override
    public List<CacheRuleResponse> getTableRules(String tableName, String datasource) {
        log.debug("开始获取表格缓存规则，表名: {}, 数据源: {}", tableName, datasource);
        
        List<CacheRule> rules = cacheRuleRepository.findByTable(tableName, datasource);
        
        List<CacheRuleResponse> responses = rules.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        log.debug("完成表格缓存规则获取，表名: {}, 规则数量: {}", tableName, responses.size());
        return responses;
    }

    @Override
    public CacheRuleResponse createTableRule(String tableName, CreateTableRuleRequest request) {
        log.debug("开始为表格创建缓存规则，表名: {}, 请求: {}", tableName, request);
        
        // 构建缓存规则
        CacheRule rule = CacheRule.builder()
            .id(UUID.randomUUID().toString())
            .name(request.getName())
            .description(request.getDescription())
            .datasourceName(request.getDatasourceName())
            .ruleType(CacheRule.RuleType.TABLE_MATCH)
            .tables(Collections.singletonList(tableName))
            .ttl(request.getTtl())
            .priority(request.getPriority())
            .enabled(request.getEnabled())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        // 保存规则
        CacheRule savedRule = cacheRuleRepository.save(rule);
        
        CacheRuleResponse response = convertToResponse(savedRule);
        
        log.debug("完成表格缓存规则创建，表名: {}, 规则ID: {}", tableName, response.getRuleId());
        return response;
    }
    
    private CacheRuleResponse convertToResponse(CacheRule rule) {
        return CacheRuleResponse.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .description(rule.getDescription())
            .datasourceName(rule.getDatasourceName())
            .dbType("mysql") // 默认值
            .ruleType(rule.getRuleType() != null ? rule.getRuleType().name().toLowerCase() : "tables")
            .tables(rule.getTables())
            .ttl(rule.getTtl())
            .priority(rule.getPriority())
            .enabled(rule.isEnabled())
            .createdAt(rule.getCreatedAt().toString())
            .updatedAt(rule.getUpdatedAt().toString())
            .matchedQueries(Collections.emptyList())
            .build();
    }
}