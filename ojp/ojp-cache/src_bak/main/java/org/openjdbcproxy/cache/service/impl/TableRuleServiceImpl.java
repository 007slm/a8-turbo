package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.openjdbcproxy.cache.dto.CreateTableRuleRequest;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.RuleType;
import org.openjdbcproxy.cache.repository.CacheRuleRepository;
import org.openjdbcproxy.cache.service.TableRuleService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * 表格缓存规则服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableRuleServiceImpl implements TableRuleService {

    private final CacheRuleRepository cacheRuleRepository;

    @Override
    public List<CacheRule> getTableRules(String tableName, String datasource) {
        log.debug("开始获取表格缓存规则，表名: {}, 数据源: {}", tableName, datasource);
        
        List<CacheRule> rules = cacheRuleRepository.findByTable(tableName, datasource);
        
        log.debug("完成表格缓存规则获取，表名: {}, 规则数量: {}", tableName, rules.size());
        return rules;
    }

    @Override
    //TODO
    public CacheRule createTableRule(String tableName, CreateTableRuleRequest rule) {
        log.debug("开始为表格创建缓存规则，表名: {}, 请求: {}", tableName, rule);
        return null;
    }

}