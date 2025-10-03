package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.repository.MyCacheRuleRepository;
import org.openjdbcproxy.cache.service.CacheRuleManagementService;
import org.springframework.beans.factory.annotation.Autowired;
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

    MyCacheRuleRepository cacheRuleRepository;

    @Autowired
    public CacheRuleController(MyCacheRuleRepository cacheRuleRepository) {
        this.cacheRuleRepository = cacheRuleRepository;
    }

    @GetMapping("/list")
    @Operation(summary = "获取缓存规则列表", description = "获取所有缓存规则")
    public List<CacheRule> getRuleList() {
        log.info("获取缓存规则列表（按数据库分组）");
        List<CacheRule> rules = cacheRuleRepository.findAll();
        log.info("成功获取缓存规则列表，数据库数量: {}", rules.size());
        return rules;
    }



    @PostMapping
    @Operation(summary = "创建更新缓存规则", description = "创建更新缓存规则")
    public CacheRule createRule(
            @Parameter(description = "缓存规则信息") @RequestBody CacheRule request) {
        return cacheRuleRepository.save(request);
    }


    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除缓存规则", description = "根据ID删除缓存规则")
    public void deleteRule(
            @Parameter(description = "规则ID") @PathVariable String ruleId) {
        cacheRuleRepository.deleteById(ruleId);
    }


}