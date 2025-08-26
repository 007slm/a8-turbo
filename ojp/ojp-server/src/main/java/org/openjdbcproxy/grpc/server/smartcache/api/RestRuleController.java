package org.openjdbcproxy.grpc.server.smartcache.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.smartcache.api.model.*;
import org.openjdbcproxy.grpc.server.smartcache.service.SmartCacheRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规则管理 RESTful 控制器
 * 提供缓存规则的完整CRUD操作，与 smart cache web api 保持一致的接口
 * 基于 OJP Server 的 SmartCacheRuleService
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RestRuleController {
    
    private final SmartCacheRuleService ruleService;

    /**
     * 获取所有规则
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleInfo>>> getRules() {
        try {
            List<CacheRuleInfo> cacheRules = ruleService.getAllRules();
            List<RuleInfo> rules = cacheRules.stream()
                    .map(RuleInfo::fromCacheRuleInfo)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success("获取规则列表成功", rules));
        } catch (Exception e) {
            log.error("获取规则列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取规则列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建新规则
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RuleInfo>> createRule(@RequestBody RuleInfo ruleInfo) {
        try {
            // 转换为 CacheRuleInfo
            CacheRuleInfo cacheRuleInfo = ruleInfo.toCacheRuleInfo();
            
            // 验证规则
            Map<String, Object> validation = ruleService.validateRule(cacheRuleInfo);
            if (!(Boolean) validation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validation.get("errors");
                return ResponseEntity.ok(ApiResponse.error("规则验证失败: " + String.join(", ", errors)));
            }
            
            CacheRuleInfo createdRule = ruleService.createRule(cacheRuleInfo);
            RuleInfo result = RuleInfo.fromCacheRuleInfo(createdRule);
            return ResponseEntity.ok(ApiResponse.success("创建规则成功", result));
            
        } catch (Exception e) {
            log.error("创建规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("创建规则失败: " + e.getMessage()));
        }
    }

    /**
     * 更新规则
     */
    @PutMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleInfo>> updateRule(
            @PathVariable String ruleId,
            @RequestBody RuleInfo ruleInfo) {
        
        try {
            // 转换为 CacheRuleInfo
            CacheRuleInfo cacheRuleInfo = ruleInfo.toCacheRuleInfo();
            
            // 验证规则
            Map<String, Object> validation = ruleService.validateRule(cacheRuleInfo);
            if (!(Boolean) validation.get("valid")) {
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) validation.get("errors");
                return ResponseEntity.ok(ApiResponse.error("规则验证失败: " + String.join(", ", errors)));
            }
            
            CacheRuleInfo updatedRule = ruleService.updateRule(ruleId, cacheRuleInfo);
            RuleInfo result = RuleInfo.fromCacheRuleInfo(updatedRule);
            return ResponseEntity.ok(ApiResponse.success("更新规则成功", result));
            
        } catch (Exception e) {
            log.error("更新规则失败: {}", ruleId, e);
            return ResponseEntity.ok(ApiResponse.error("更新规则失败: " + e.getMessage()));
        }
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteRule(@PathVariable String ruleId) {
        try {
            boolean deleted = ruleService.deleteRule(ruleId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("删除规则成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("规则不存在"));
            }
        } catch (Exception e) {
            log.error("删除规则失败: {}", ruleId, e);
            return ResponseEntity.ok(ApiResponse.error("删除规则失败: " + e.getMessage()));
        }
    }

    /**
     * 批量提交规则
     */
    @PostMapping("/commit")
    public ResponseEntity<ApiResponse<Boolean>> commitRules(@RequestBody Map<String, List<RuleInfo>> request) {
        try {
            List<RuleInfo> rules = request.get("rules");
            if (rules == null) {
                return ResponseEntity.ok(ApiResponse.error("规则列表不能为空"));
            }
            
            // 转换为 CacheRuleInfo 列表
            List<CacheRuleInfo> cacheRules = rules.stream()
                    .map(RuleInfo::toCacheRuleInfo)
                    .collect(Collectors.toList());
            
            // 验证所有规则
            for (int i = 0; i < cacheRules.size(); i++) {
                CacheRuleInfo rule = cacheRules.get(i);
                Map<String, Object> validation = ruleService.validateRule(rule);
                if (!(Boolean) validation.get("valid")) {
                    @SuppressWarnings("unchecked")
                    List<String> errors = (List<String>) validation.get("errors");
                    return ResponseEntity.ok(ApiResponse.error(
                        "规则 " + (i + 1) + " 验证失败: " + String.join(", ", errors)));
                }
            }
            
            BatchRuleResult result = ruleService.batchSubmitRules(cacheRules);
            if (result != null && result.getSuccessCount() > 0) {
                return ResponseEntity.ok(ApiResponse.success("批量提交规则成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("批量提交规则失败"));
            }
            
        } catch (Exception e) {
            log.error("批量提交规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量提交规则失败: " + e.getMessage()));
        }
    }

    /**
     * 验证规则
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateRule(@RequestBody RuleInfo ruleInfo) {
        try {
            CacheRuleInfo cacheRuleInfo = ruleInfo.toCacheRuleInfo();
            Map<String, Object> validation = ruleService.validateRule(cacheRuleInfo);
            return ResponseEntity.ok(ApiResponse.success("规则验证完成", validation));
        } catch (Exception e) {
            log.error("规则验证失败", e);
            return ResponseEntity.ok(ApiResponse.error("规则验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取规则统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRuleStats() {
        try {
            List<CacheRuleInfo> rules = ruleService.getAllRules();
            
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
            log.error("获取规则统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取规则统计失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索规则
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RuleInfo>>> searchRules(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "all") String searchType) {
        
        try {
            List<CacheRuleInfo> allRules = ruleService.getAllRules();
            
            List<RuleInfo> filteredRules = allRules.stream()
                .map(RuleInfo::fromCacheRuleInfo)
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
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("搜索规则成功", filteredRules));
            
        } catch (Exception e) {
            log.error("搜索规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("搜索规则失败: " + e.getMessage()));
        }
    }

    /**
     * 复制规则
     */
    @PostMapping("/{ruleId}/copy")
    public ResponseEntity<ApiResponse<RuleInfo>> copyRule(
            @PathVariable String ruleId,
            @RequestBody(required = false) Map<String, String> modifications) {
        
        try {
            CacheRuleInfo originalRule = ruleService.getRuleById(ruleId);
            if (originalRule == null) {
                return ResponseEntity.ok(ApiResponse.error("规则不存在"));
            }
            
            // 创建副本
            CacheRuleInfo copiedRule = CacheRuleInfo.builder()
                .ttl(originalRule.getTtl())
                .ruleType(originalRule.getRuleType())
                .tables(originalRule.getTables())
                .tablesAny(originalRule.getTablesAny())
                .tablesAll(originalRule.getTablesAll())
                .regex(originalRule.getRegex())
                .queryIds(originalRule.getQueryIds())
                .priority(originalRule.getPriority())
                .build();
            
            // 应用修改
            if (modifications != null) {
                if (modifications.containsKey("ttl")) {
                    copiedRule.setTtl(modifications.get("ttl"));
                }
                // 可以添加更多修改选项
            }
            
            CacheRuleInfo createdRule = ruleService.createRule(copiedRule);
            RuleInfo result = RuleInfo.fromCacheRuleInfo(createdRule);
            return ResponseEntity.ok(ApiResponse.success("复制规则成功", result));
            
        } catch (Exception e) {
            log.error("复制规则失败: {}", ruleId, e);
            return ResponseEntity.ok(ApiResponse.error("复制规则失败: " + e.getMessage()));
        }
    }
}
