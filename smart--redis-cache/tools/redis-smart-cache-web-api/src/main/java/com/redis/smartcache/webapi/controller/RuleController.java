package com.redis.smartcache.webapi.controller;

import com.redis.smartcache.webapi.model.ApiResponse;
import com.redis.smartcache.webapi.model.RuleInfo;
import com.redis.smartcache.webapi.service.RedisSmartCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 规则管理控制器
 * 提供缓存规则的完整CRUD操作
 */
@RestController
@RequestMapping("/api/rules")
@Tag(name = "规则管理", description = "缓存规则相关的API接口")
@CrossOrigin(origins = "*")
public class RuleController {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleController.class);
    
    @Autowired
    private RedisSmartCacheService redisService;

    /**
     * 获取所有规则
     */
    @GetMapping
    @Operation(summary = "获取规则列表", description = "获取所有缓存规则")
    public ResponseEntity<ApiResponse<List<RuleInfo>>> getRules() {
        try {
            List<RuleInfo> rules = redisService.getRules();
            return ResponseEntity.ok(ApiResponse.success("获取规则列表成功", rules));
        } catch (Exception e) {
            logger.error("获取规则列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取规则列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新规则
     */
    @PostMapping
    @Operation(summary = "创建规则", description = "创建新的缓存规则")
    public ResponseEntity<ApiResponse<RuleInfo>> createRule(
            @RequestBody RuleInfo ruleInfo) {
        
        try {
            // 验证规则
            Map<String, Object> validation = redisService.validateRule(ruleInfo);
            if (!(Boolean) validation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validation.get("errors");
                return ResponseEntity.ok(ApiResponse.error("规则验证失败: " + String.join(", ", errors)));
            }
            
            RuleInfo createdRule = redisService.createRule(ruleInfo);
            return ResponseEntity.ok(ApiResponse.success("创建规则成功", createdRule));
            
        } catch (Exception e) {
            logger.error("创建规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("创建规则失败: " + e.getMessage()));
        }
    }

    /**
     * 更新规则
     */
    @PutMapping("/{ruleId}")
    @Operation(summary = "更新规则", description = "更新指定的缓存规则")
    public ResponseEntity<ApiResponse<RuleInfo>> updateRule(
            @Parameter(description = "规则ID")
            @PathVariable String ruleId,
            
            @RequestBody RuleInfo ruleInfo) {
        
        try {
            // 验证规则
            Map<String, Object> validation = redisService.validateRule(ruleInfo);
            if (!(Boolean) validation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validation.get("errors");
                return ResponseEntity.ok(ApiResponse.error("规则验证失败: " + String.join(", ", errors)));
            }
            
            RuleInfo updatedRule = redisService.updateRule(ruleId, ruleInfo);
            return ResponseEntity.ok(ApiResponse.success("更新规则成功", updatedRule));
            
        } catch (Exception e) {
            logger.error("更新规则失败: " + ruleId, e);
            return ResponseEntity.ok(ApiResponse.error("更新规则失败: " + e.getMessage()));
        }
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除规则", description = "删除指定的缓存规则")
    public ResponseEntity<ApiResponse<Boolean>> deleteRule(
            @Parameter(description = "规则ID")
            @PathVariable String ruleId) {
        
        try {
            boolean deleted = redisService.deleteRule(ruleId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("删除规则成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("删除规则失败"));
            }
        } catch (Exception e) {
            logger.error("删除规则失败: " + ruleId, e);
            return ResponseEntity.ok(ApiResponse.error("删除规则失败: " + e.getMessage()));
        }
    }

    /**
     * 批量提交规则
     */
    @PostMapping("/commit")
    @Operation(summary = "批量提交规则", description = "批量提交多个缓存规则")
    public ResponseEntity<ApiResponse<Boolean>> commitRules(
            @RequestBody Map<String, List<RuleInfo>> request) {
        
        try {
            List<RuleInfo> rules = request.get("rules");
            if (rules == null) {
                return ResponseEntity.ok(ApiResponse.error("规则列表不能为空"));
            }
            
            // 验证所有规则
            for (int i = 0; i < rules.size(); i++) {
                RuleInfo rule = rules.get(i);
                Map<String, Object> validation = redisService.validateRule(rule);
                if (!(Boolean) validation.get("valid")) {
                    @SuppressWarnings("unchecked")
                    List<String> errors = (List<String>) validation.get("errors");
                    return ResponseEntity.ok(ApiResponse.error(
                        "规则 " + (i + 1) + " 验证失败: " + String.join(", ", errors)));
                }
            }
            
            boolean committed = redisService.commitRules(rules);
            if (committed) {
                return ResponseEntity.ok(ApiResponse.success("批量提交规则成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("批量提交规则失败"));
            }
            
        } catch (Exception e) {
            logger.error("批量提交规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量提交规则失败: " + e.getMessage()));
        }
    }

    /**
     * 验证规则
     */
    @PostMapping("/validate")
    @Operation(summary = "验证规则", description = "验证规则配置是否正确")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateRule(
            @RequestBody RuleInfo ruleInfo) {
        
        try {
            Map<String, Object> validation = redisService.validateRule(ruleInfo);
            return ResponseEntity.ok(ApiResponse.success("规则验证完成", validation));
        } catch (Exception e) {
            logger.error("规则验证失败", e);
            return ResponseEntity.ok(ApiResponse.error("规则验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取规则统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取规则统计", description = "获取规则的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRuleStats() {
        try {
            List<RuleInfo> rules = redisService.getRules();
            
            long totalRules = rules.size();
            long tablesRules = rules.stream().mapToLong(r -> 
                (r.getTables() != null && !r.getTables().isEmpty()) ||
                (r.getTablesAny() != null && !r.getTablesAny().isEmpty()) ||
                (r.getTablesAll() != null && !r.getTablesAll().isEmpty()) ? 1 : 0).sum();
            long queryRules = rules.stream().mapToLong(r -> 
                r.getQueryIds() != null && !r.getQueryIds().isEmpty() ? 1 : 0).sum();
            long regexRules = rules.stream().mapToLong(r -> 
                r.getRegex() != null && !r.getRegex().trim().isEmpty() ? 1 : 0).sum();
            long globalRules = totalRules - tablesRules - queryRules - regexRules;
            
            Map<String, Object> stats = Map.of(
                "totalRules", totalRules,
                "tablesRules", tablesRules,
                "queryRules", queryRules,
                "regexRules", regexRules,
                "globalRules", globalRules,
                "ruleTypes", Map.of(
                    "Tables", tablesRules,
                    "QueryIds", queryRules,
                    "Regex", regexRules,
                    "Global", globalRules
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取规则统计成功", stats));
            
        } catch (Exception e) {
            logger.error("获取规则统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取规则统计失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索规则
     */
    @GetMapping("/search")
    @Operation(summary = "搜索规则", description = "根据关键词搜索规则")
    public ResponseEntity<ApiResponse<List<RuleInfo>>> searchRules(
            @Parameter(description = "搜索关键词")
            @RequestParam String keyword,
            
            @Parameter(description = "搜索类型 (ttl, tables, regex, all)")
            @RequestParam(required = false, defaultValue = "all") String searchType) {
        
        try {
            List<RuleInfo> allRules = redisService.getRules();
            
            List<RuleInfo> filteredRules = allRules.stream()
                .filter(rule -> {
                    String lowerKeyword = keyword.toLowerCase();
                    
                    switch (searchType.toLowerCase()) {
                        case "ttl":
                            return rule.getTtl() != null && 
                                   rule.getTtl().toLowerCase().contains(lowerKeyword);
                        case "tables":
                            return (rule.getTables() != null && 
                                   rule.getTables().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword))) ||
                                   (rule.getTablesAny() != null && 
                                   rule.getTablesAny().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword))) ||
                                   (rule.getTablesAll() != null && 
                                   rule.getTablesAll().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword)));
                        case "regex":
                            return rule.getRegex() != null && 
                                   rule.getRegex().toLowerCase().contains(lowerKeyword);
                        default: // all
                            boolean ttlMatch = rule.getTtl() != null && 
                                             rule.getTtl().toLowerCase().contains(lowerKeyword);
                            boolean tablesMatch = (rule.getTables() != null && 
                                                 rule.getTables().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword))) ||
                                                 (rule.getTablesAny() != null && 
                                                 rule.getTablesAny().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword))) ||
                                                 (rule.getTablesAll() != null && 
                                                 rule.getTablesAll().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword)));
                            boolean regexMatch = rule.getRegex() != null && 
                                               rule.getRegex().toLowerCase().contains(lowerKeyword);
                            boolean queryIdsMatch = rule.getQueryIds() != null && 
                                                  rule.getQueryIds().stream().anyMatch(q -> q.toLowerCase().contains(lowerKeyword));
                            
                            return ttlMatch || tablesMatch || regexMatch || queryIdsMatch;
                    }
                })
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("搜索规则成功", filteredRules));
            
        } catch (Exception e) {
            logger.error("搜索规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("搜索规则失败: " + e.getMessage()));
        }
    }

    /**
     * 复制规则
     */
    @PostMapping("/{ruleId}/copy")
    @Operation(summary = "复制规则", description = "复制指定规则创建新规则")
    public ResponseEntity<ApiResponse<RuleInfo>> copyRule(
            @Parameter(description = "源规则ID")
            @PathVariable String ruleId,
            
            @RequestBody(required = false) Map<String, String> modifications) {
        
        try {
            List<RuleInfo> rules = redisService.getRules();
            int index = Integer.parseInt(ruleId);
            
            if (index < 0 || index >= rules.size()) {
                return ResponseEntity.ok(ApiResponse.error("规则不存在"));
            }
            
            RuleInfo originalRule = rules.get(index);
            
            // 创建副本
            RuleInfo copiedRule = RuleInfo.builder()
                .ttl(originalRule.getTtl())
                .ruleType(originalRule.getRuleType())
                .tables(originalRule.getTables())
                .tablesAny(originalRule.getTablesAny())
                .tablesAll(originalRule.getTablesAll())
                .regex(originalRule.getRegex())
                .queryIds(originalRule.getQueryIds())
                .build();
            
            // 应用修改
            if (modifications != null) {
                if (modifications.containsKey("ttl")) {
                    copiedRule.setTtl(modifications.get("ttl"));
                }
                // 可以添加更多修改选项
            }
            
            RuleInfo createdRule = redisService.createRule(copiedRule);
            return ResponseEntity.ok(ApiResponse.success("复制规则成功", createdRule));
            
        } catch (Exception e) {
            logger.error("复制规则失败: " + ruleId, e);
            return ResponseEntity.ok(ApiResponse.error("复制规则失败: " + e.getMessage()));
        }
    }
}