package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.ApiResponse;
import org.openjdbcproxy.grpc.server.dto.CacheStatsDto;
import org.openjdbcproxy.grpc.server.interceptor.impl.cache.rule.CacheRule;
import org.openjdbcproxy.grpc.server.service.CacheRuleService;
import org.openjdbcproxy.grpc.server.service.CacheStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存规则管理和统计信息的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
public class CacheController {

    @Autowired
    private CacheRuleService cacheRuleService;
    
    @Autowired
    private CacheStatsService cacheStatsService;

    // ==================== 缓存规则管理 ====================

    /**
     * 获取所有缓存规则
     * GET /api/cache/rules
     */
    @GetMapping("/rules")
    public ApiResponse<List<CacheRule>> getAllRules() {
        try {
            List<CacheRule> rules = cacheRuleService.getAllRules();
            return ApiResponse.success(rules, "获取缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get cache rules", e);
            return ApiResponse.error("GET_RULES_ERROR", "获取缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取缓存规则
     * GET /api/cache/rules/{id}
     */
    @GetMapping("/rules/{id}")
    public ApiResponse<CacheRule> getRuleById(@PathVariable String id) {
        try {
            return cacheRuleService.getRuleById(id)
                    .map(rule -> ApiResponse.success(rule, "获取缓存规则成功"))
                    .orElse(ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + id));
        } catch (Exception e) {
            log.error("Failed to get cache rule: {}", id, e);
            return ApiResponse.error("GET_RULE_ERROR", "获取缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建缓存规则
     * POST /api/cache/rules
     */
    @PostMapping("/rules")
    public ApiResponse<CacheRule> createRule(@RequestBody CacheRule rule) {
        try {
            CacheRule createdRule = cacheRuleService.createRule(rule);
            return ApiResponse.success(createdRule, "创建缓存规则成功");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cache rule: {}", e.getMessage());
            return ApiResponse.error("INVALID_RULE", "无效的缓存规则: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create cache rule", e);
            return ApiResponse.error("CREATE_RULE_ERROR", "创建缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 更新缓存规则
     * PUT /api/cache/rules/{id}
     */
    @PutMapping("/rules/{id}")
    public ApiResponse<CacheRule> updateRule(@PathVariable String id, @RequestBody CacheRule rule) {
        try {
            CacheRule updatedRule = cacheRuleService.updateRule(id, rule);
            return ApiResponse.success(updatedRule, "更新缓存规则成功");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cache rule update: {}", e.getMessage());
            return ApiResponse.error("INVALID_RULE", "无效的缓存规则: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update cache rule: {}", id, e);
            return ApiResponse.error("UPDATE_RULE_ERROR", "更新缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存规则
     * DELETE /api/cache/rules/{id}
     */
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Boolean> deleteRule(@PathVariable String id) {
        try {
            boolean deleted = cacheRuleService.deleteRule(id);
            if (deleted) {
                return ApiResponse.success(true, "删除缓存规则成功");
            } else {
                return ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + id);
            }
        } catch (Exception e) {
            log.error("Failed to delete cache rule: {}", id, e);
            return ApiResponse.error("DELETE_RULE_ERROR", "删除缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 启用缓存规则
     * POST /api/cache/rules/{id}/enable
     */
    @PostMapping("/rules/{id}/enable")
    public ApiResponse<CacheRule> enableRule(@PathVariable String id) {
        try {
            CacheRule rule = cacheRuleService.toggleRule(id, true);
            return ApiResponse.success(rule, "启用缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to enable cache rule: {}", id, e);
            return ApiResponse.error("ENABLE_RULE_ERROR", "启用缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 禁用缓存规则
     * POST /api/cache/rules/{id}/disable
     */
    @PostMapping("/rules/{id}/disable")
    public ApiResponse<CacheRule> disableRule(@PathVariable String id) {
        try {
            CacheRule rule = cacheRuleService.toggleRule(id, false);
            return ApiResponse.success(rule, "禁用缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to disable cache rule: {}", id, e);
            return ApiResponse.error("DISABLE_RULE_ERROR", "禁用缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 验证缓存规则
     * POST /api/cache/rules/validate
     */
    @PostMapping("/rules/validate")
    public ApiResponse<CacheStatsDto.RuleValidationResult> validateRules(@RequestBody(required = false) CacheRule rule) {
        try {
            CacheStatsDto.RuleValidationResult result;
            if (rule != null) {
                result = cacheRuleService.validateRule(rule);
            } else {
                result = cacheRuleService.validateAllRules();
            }
            return ApiResponse.success(result, result.isValid() ? "规则验证通过" : "规则验证失败");
        } catch (Exception e) {
            log.error("Failed to validate rules", e);
            return ApiResponse.error("VALIDATE_RULES_ERROR", "规则验证失败: " + e.getMessage());
        }
    }

    /**
     * 提交缓存规则更改
     * POST /api/cache/rules/commit
     */
    @PostMapping("/rules/commit")
    public ApiResponse<String> commitRules(@RequestBody(required = false) List<String> ruleIds) {
        try {
            // 这里可以实现批量提交逻辑，目前简单返回成功
            return ApiResponse.success("Rules committed successfully", "缓存规则提交成功");
        } catch (Exception e) {
            log.error("Failed to commit rules", e);
            return ApiResponse.error("COMMIT_RULES_ERROR", "提交缓存规则失败: " + e.getMessage());
        }
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取缓存概览统计
     * GET /api/cache/stats/overview
     */
    @GetMapping("/stats/overview")
    public ApiResponse<CacheStatsDto.OverviewStats> getOverviewStats() {
        try {
            CacheStatsDto.OverviewStats stats = cacheStatsService.getOverviewStats();
            return ApiResponse.success(stats, "获取缓存概览统计成功");
        } catch (Exception e) {
            log.error("Failed to get overview stats", e);
            return ApiResponse.error("GET_OVERVIEW_STATS_ERROR", "获取缓存概览统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存命中率统计
     * GET /api/cache/stats/hit-rate
     */
    @GetMapping("/stats/hit-rate")
    public ApiResponse<CacheStatsDto.HitRateStats> getHitRateStats(
            @RequestParam(required = false, defaultValue = "24h") String timeRange) {
        try {
            CacheStatsDto.HitRateStats stats = cacheStatsService.getHitRateStats(timeRange);
            return ApiResponse.success(stats, "获取缓存命中率统计成功");
        } catch (Exception e) {
            log.error("Failed to get hit rate stats", e);
            return ApiResponse.error("GET_HIT_RATE_STATS_ERROR", "获取缓存命中率统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取查询性能统计
     * GET /api/cache/stats/query-performance
     */
    @GetMapping("/stats/query-performance")
    public ApiResponse<CacheStatsDto.QueryPerformanceStats> getQueryPerformanceStats(
            @RequestParam(required = false, defaultValue = "24h") String timeRange) {
        try {
            CacheStatsDto.QueryPerformanceStats stats = cacheStatsService.getQueryPerformanceStats(timeRange);
            return ApiResponse.success(stats, "获取查询性能统计成功");
        } catch (Exception e) {
            log.error("Failed to get query performance stats", e);
            return ApiResponse.error("GET_QUERY_PERFORMANCE_STATS_ERROR", "获取查询性能统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门表格统计
     * GET /api/cache/stats/top-tables
     */
    @GetMapping("/stats/top-tables")
    public ApiResponse<List<CacheStatsDto.TopTableStats>> getTopTablesStats() {
        try {
            List<CacheStatsDto.TopTableStats> stats = cacheStatsService.getTopTablesStats();
            return ApiResponse.success(stats, "获取热门表格统计成功");
        } catch (Exception e) {
            log.error("Failed to get top tables stats", e);
            return ApiResponse.error("GET_TOP_TABLES_STATS_ERROR", "获取热门表格统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取慢查询统计
     * GET /api/cache/stats/slow-queries
     */
    @GetMapping("/stats/slow-queries")
    public ApiResponse<List<CacheStatsDto.SlowQueryStats>> getSlowQueriesStats() {
        try {
            List<CacheStatsDto.SlowQueryStats> stats = cacheStatsService.getSlowQueriesStats();
            return ApiResponse.success(stats, "获取慢查询统计成功");
        } catch (Exception e) {
            log.error("Failed to get slow queries stats", e);
            return ApiResponse.error("GET_SLOW_QUERIES_STATS_ERROR", "获取慢查询统计失败: " + e.getMessage());
        }
    }

    // ==================== 查询管理 ====================

    /**
     * 获取查询列表
     * GET /api/cache/queries
     */
    @GetMapping("/queries")
    public ApiResponse<List<CacheStatsDto.QueryInfo>> getQueries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String field,
            @RequestParam(required = false, defaultValue = "desc") String direction) {
        try {
            List<CacheStatsDto.QueryInfo> queries = cacheStatsService.getQueries(search, field, direction, null);
            return ApiResponse.success(queries, "获取查询列表成功");
        } catch (Exception e) {
            log.error("Failed to get queries", e);
            return ApiResponse.error("GET_QUERIES_ERROR", "获取查询列表失败: " + e.getMessage());
        }
    }

    /**
     * 为查询创建缓存规则
     * POST /api/cache/queries/create-rule
     */
    @PostMapping("/queries/create-rule")
    public ApiResponse<CacheRule> createQueryRule(@RequestBody Map<String, Object> request) {
        try {
            String queryId = (String) request.get("queryId");
            String ttl = (String) request.get("ttl");
            String description = (String) request.get("description");
            
            CacheRule rule = cacheRuleService.createQueryIdsRule(
                "Query Rule for " + queryId,
                description != null ? description : "为查询 " + queryId + " 创建的缓存规则",
                List.of(queryId),
                ttl
            );
            
            return ApiResponse.success(rule, "为查询创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create query rule", e);
            return ApiResponse.error("CREATE_QUERY_RULE_ERROR", "为查询创建缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取特定查询的缓存规则
     * GET /api/cache/queries/{queryId}/rules
     */
    @GetMapping("/queries/{queryId}/rules")
    public ApiResponse<List<CacheRule>> getQueryRules(@PathVariable String queryId) {
        try {
            List<CacheRule> rules = cacheRuleService.getRulesByQueryId(queryId);
            return ApiResponse.success(rules, "获取查询缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get query rules: {}", queryId, e);
            return ApiResponse.error("GET_QUERY_RULES_ERROR", "获取查询缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 为特定查询创建缓存规则
     * POST /api/cache/queries/{queryId}/rules
     */
    @PostMapping("/queries/{queryId}/rules")
    public ApiResponse<CacheRule> createQueryRuleForId(@PathVariable String queryId, @RequestBody Map<String, Object> request) {
        try {
            String ttl = (String) request.get("ttl");
            String description = (String) request.get("description");
            
            CacheRule rule = cacheRuleService.createQueryIdsRule(
                "Query Rule for " + queryId,
                description != null ? description : "为查询 " + queryId + " 创建的缓存规则",
                List.of(queryId),
                ttl
            );
            
            return ApiResponse.success(rule, "为查询创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create query rule: {}", queryId, e);
            return ApiResponse.error("CREATE_QUERY_RULE_ERROR", "为查询创建缓存规则失败: " + e.getMessage());
        }
    }

    // ==================== 表格管理 ====================

    /**
     * 获取表格列表
     * GET /api/cache/tables
     */
    @GetMapping("/tables")
    public ApiResponse<List<CacheStatsDto.TableInfo>> getTables(
            @RequestParam(required = false) String search) {
        try {
            List<CacheStatsDto.TableInfo> tables = cacheStatsService.getTables(search);
            return ApiResponse.success(tables, "获取表格列表成功");
        } catch (Exception e) {
            log.error("Failed to get tables", e);
            return ApiResponse.error("GET_TABLES_ERROR", "获取表格列表失败: " + e.getMessage());
        }
    }

    /**
     * 为表格创建缓存规则
     * POST /api/cache/tables/create-rule
     */
    @PostMapping("/tables/create-rule")
    public ApiResponse<CacheRule> createTableRule(@RequestBody Map<String, Object> request) {
        try {
            String tableName = (String) request.get("tableName");
            String ttl = (String) request.get("ttl");
            String description = (String) request.get("description");
            
            CacheRule rule = cacheRuleService.createTablesRule(
                "Table Rule for " + tableName,
                description != null ? description : "为表格 " + tableName + " 创建的缓存规则",
                List.of(tableName),
                ttl
            );
            
            return ApiResponse.success(rule, "为表格创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create table rule", e);
            return ApiResponse.error("CREATE_TABLE_RULE_ERROR", "为表格创建缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取特定表格的缓存规则
     * GET /api/cache/tables/{tableName}/rules
     */
    @GetMapping("/tables/{tableName}/rules")
    public ApiResponse<List<CacheRule>> getTableRules(@PathVariable String tableName) {
        try {
            List<CacheRule> rules = cacheRuleService.getRulesByTableName(tableName);
            return ApiResponse.success(rules, "获取表格缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get table rules: {}", tableName, e);
            return ApiResponse.error("GET_TABLE_RULES_ERROR", "获取表格缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 为特定表格创建缓存规则
     * POST /api/cache/tables/{tableName}/rules
     */
    @PostMapping("/tables/{tableName}/rules")
    public ApiResponse<CacheRule> createTableRuleForName(@PathVariable String tableName, @RequestBody Map<String, Object> request) {
        try {
            String ttl = (String) request.get("ttl");
            String description = (String) request.get("description");
            
            CacheRule rule = cacheRuleService.createTablesRule(
                "Table Rule for " + tableName,
                description != null ? description : "为表格 " + tableName + " 创建的缓存规则",
                List.of(tableName),
                ttl
            );
            
            return ApiResponse.success(rule, "为表格创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create table rule: {}", tableName, e);
            return ApiResponse.error("CREATE_TABLE_RULE_ERROR", "为表格创建缓存规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建正则表达式规则
     * POST /api/cache/rules/regex
     */
    @PostMapping("/rules/regex")
    public ApiResponse<CacheRule> createRegexRule(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String regex = (String) request.get("regex");
            String ttl = (String) request.get("ttl");
            
            CacheRule rule = cacheRuleService.createRegexRule(name, description, regex, ttl);
            return ApiResponse.success(rule, "创建正则表达式规则成功");
        } catch (Exception e) {
            log.error("Failed to create regex rule", e);
            return ApiResponse.error("CREATE_REGEX_RULE_ERROR", "创建正则表达式规则失败: " + e.getMessage());
        }
    }

    /**
     * 创建全局规则
     * POST /api/cache/rules/global
     */
    @PostMapping("/rules/global")
    public ApiResponse<CacheRule> createGlobalRule(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String ttl = (String) request.get("ttl");
            
            CacheRule rule = cacheRuleService.createAnyRule(name, description, ttl);
            return ApiResponse.success(rule, "创建全局规则成功");
        } catch (Exception e) {
            log.error("Failed to create global rule", e);
            return ApiResponse.error("CREATE_GLOBAL_RULE_ERROR", "创建全局规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取表格统计
     * GET /api/cache/tables/{tableName}/stats
     */
    @GetMapping("/tables/{tableName}/stats")
    public ApiResponse<CacheStatsDto.TableInfo> getTableStats(@PathVariable String tableName) {
        try {
            List<CacheStatsDto.TableInfo> tables = cacheStatsService.getTables(null);
            return tables.stream()
                    .filter(table -> table.getName().equals(tableName))
                    .findFirst()
                    .map(table -> ApiResponse.success(table, "获取表格统计成功"))
                    .orElse(ApiResponse.error("TABLE_NOT_FOUND", "表格不存在: " + tableName));
        } catch (Exception e) {
            log.error("Failed to get table stats: {}", tableName, e);
            return ApiResponse.error("GET_TABLE_STATS_ERROR", "获取表格统计失败: " + e.getMessage());
        }
    }

    // ==================== 系统信息 ====================

    /**
     * 获取规则统计信息
     * GET /api/cache/rules/stats
     */
    @GetMapping("/rules/stats")
    public ApiResponse<Map<String, Object>> getRuleStats() {
        try {
            Map<String, Object> stats = Map.of(
                "totalRules", cacheRuleService.getAllRules().size(),
                "activeRules", cacheRuleService.getActiveRulesCount(),
                "ruleTypeStats", cacheRuleService.getRuleTypeStats(),
                "rulesVersion", cacheRuleService.getRulesVersion()
            );
            return ApiResponse.success(stats, "获取规则统计成功");
        } catch (Exception e) {
            log.error("Failed to get rule stats", e);
            return ApiResponse.error("GET_RULE_STATS_ERROR", "获取规则统计失败: " + e.getMessage());
        }
    }
}
