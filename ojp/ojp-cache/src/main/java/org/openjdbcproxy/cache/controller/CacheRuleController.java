package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.CacheRuleResponse;
import org.openjdbcproxy.cache.dto.CreateCacheRuleRequest;
import org.openjdbcproxy.cache.dto.UpdateCacheRuleRequest;
import org.openjdbcproxy.cache.service.CacheRuleManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存规则管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/rules")
@Tag(name = "Cache Rule Management", description = "缓存规则管理API")
@RequiredArgsConstructor
public class CacheRuleController {

    private final CacheRuleManagementService cacheRuleManagementService;

    @GetMapping("/list")
    @Operation(summary = "获取缓存规则列表", description = "按数据库分组获取所有缓存规则")
    public Map<String, List<CacheRuleResponse>> getRuleList() {
        log.info("获取缓存规则列表（按数据库分组）");
        Map<String, List<CacheRuleResponse>> rules = cacheRuleManagementService.getRules();
        log.info("成功获取缓存规则列表，数据库数量: {}", rules.size());
        return rules;
    }



    @PostMapping
    @Operation(summary = "创建缓存规则", description = "创建新的缓存规则")
    public CacheRuleResponse createRule(
            @Parameter(description = "缓存规则信息") @Valid @RequestBody CreateCacheRuleRequest request) {
        return cacheRuleManagementService.createRule(request);
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "更新缓存规则", description = "根据ID更新缓存规则")
    public CacheRuleResponse updateRule(
            @Parameter(description = "规则ID") @PathVariable String ruleId,
            @Parameter(description = "缓存规则信息") @Valid @RequestBody UpdateCacheRuleRequest request) {
        return cacheRuleManagementService.updateRule(ruleId, request);
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除缓存规则", description = "根据ID删除缓存规则")
    public void deleteRule(
            @Parameter(description = "规则ID") @PathVariable String ruleId) {
        cacheRuleManagementService.deleteRule(ruleId);
    }


}