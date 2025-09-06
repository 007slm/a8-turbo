package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.CacheRuleResponse;
import org.openjdbcproxy.cache.dto.CreateTableRuleRequest;
import org.openjdbcproxy.cache.service.TableRuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表格缓存规则管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/tables")
@Tag(name = "Table Cache Rule Management", description = "表格缓存规则管理API")
@RequiredArgsConstructor
public class TableRuleController {

    private final TableRuleService tableRuleService;

    @GetMapping("/{tableName}/rules")
    @Operation(summary = "获取表格缓存规则", description = "获取指定表格的所有缓存规则")
    public List<CacheRuleResponse> getTableRules(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Parameter(description = "数据源名称") @RequestParam(required = false) String datasource) {
        log.info("获取表格缓存规则，表名: {}, 数据源: {}", tableName, datasource);
        List<CacheRuleResponse> rules = tableRuleService.getTableRules(tableName, datasource);
        log.info("成功获取表格缓存规则，表名: {}, 规则数量: {}", tableName, rules.size());
        return rules;
    }

    @PostMapping("/{tableName}/rules")
    @Operation(summary = "为表格创建缓存规则", description = "为指定表格创建新的缓存规则")
    public CacheRuleResponse createTableRule(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Valid @RequestBody CreateTableRuleRequest request) {
        log.info("为表格创建缓存规则，表名: {}, 请求: {}", tableName, request);
        CacheRuleResponse rule = tableRuleService.createTableRule(tableName, request);
        log.info("成功为表格创建缓存规则，表名: {}, 规则ID: {}", tableName, rule.getRuleId());
        return rule;
    }
}