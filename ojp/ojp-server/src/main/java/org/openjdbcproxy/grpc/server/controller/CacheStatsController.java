package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.ApiResponse;
import org.openjdbcproxy.grpc.server.dto.CacheStatsDto;
import org.openjdbcproxy.grpc.server.service.CacheStatsService;
import org.openjdbcproxy.grpc.server.service.CacheRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 缓存统计控制器
 * 提供所有缓存统计相关的API端点，使用强类型返回值
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
public class CacheStatsController {
    
    @Autowired
    private CacheStatsService cacheStatsService;
    
    @Autowired
    private CacheRuleService cacheRuleService;
    
    // ====================== 缓存统计API ======================
    
    /**
     * 缓存概览统计
     * GET /api/cache/stats/overview
     */
    @GetMapping("/stats/overview")
    public ApiResponse<CacheStatsDto.CacheOverview> getCacheOverview() {
        try {
            log.debug("Getting cache overview statistics");
            CacheStatsDto.CacheOverview overview = cacheStatsService.getCacheOverview();
            return ApiResponse.success(overview, "获取缓存概览统计成功");
        } catch (Exception e) {
            log.error("Failed to get cache overview statistics", e);
            return ApiResponse.error("CACHE_OVERVIEW_ERROR", "获取缓存概览统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 缓存命中率统计
     * GET /api/cache/stats/hit-rate
     */
    @GetMapping("/stats/hit-rate")
    public ApiResponse<CacheStatsDto.HitRateStats> getHitRateStats() {
        try {
            log.debug("Getting cache hit rate statistics");
            CacheStatsDto.HitRateStats hitRateStats = cacheStatsService.getHitRateStats();
            return ApiResponse.success(hitRateStats, "获取缓存命中率统计成功");
        } catch (Exception e) {
            log.error("Failed to get cache hit rate statistics", e);
            return ApiResponse.error("HIT_RATE_ERROR", "获取缓存命中率统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询性能统计
     * GET /api/cache/stats/query-performance
     */
    @GetMapping("/stats/query-performance")
    public ApiResponse<CacheStatsDto.QueryPerformanceStats> getQueryPerformanceStats() {
        try {
            log.debug("Getting query performance statistics");
            CacheStatsDto.QueryPerformanceStats performanceStats = cacheStatsService.getQueryPerformanceStats();
            return ApiResponse.success(performanceStats, "获取查询性能统计成功");
        } catch (Exception e) {
            log.error("Failed to get query performance statistics", e);
            return ApiResponse.error("PERFORMANCE_ERROR", "获取查询性能统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 热门表格统计
     * GET /api/cache/stats/top-tables
     */
    @GetMapping("/stats/top-tables")
    public ApiResponse<CacheStatsDto.PopularTablesStats> getPopularTablesStats() {
        try {
            log.debug("Getting popular tables statistics");
            CacheStatsDto.PopularTablesStats popularTablesStats = cacheStatsService.getPopularTablesStats();
            return ApiResponse.success(popularTablesStats, "获取热门表格统计成功");
        } catch (Exception e) {
            log.error("Failed to get popular tables statistics", e);
            return ApiResponse.error("POPULAR_TABLES_ERROR", "获取热门表格统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 慢查询统计
     * GET /api/cache/stats/slow-queries
     */
    @GetMapping("/stats/slow-queries")
    public ApiResponse<CacheStatsDto.SlowQueryStats> getSlowQueryStats() {
        try {
            log.debug("Getting slow query statistics");
            CacheStatsDto.SlowQueryStats slowQueryStats = cacheStatsService.getSlowQueryStats();
            return ApiResponse.success(slowQueryStats, "获取慢查询统计成功");
        } catch (Exception e) {
            log.error("Failed to get slow query statistics", e);
            return ApiResponse.error("SLOW_QUERIES_ERROR", "获取慢查询统计失败: " + e.getMessage());
        }
    }
    
    // ====================== 查询管理API ======================
    
    /**
     * 获取查询列表
     * GET /api/cache/queries
     */
    @GetMapping("/queries")
    public ApiResponse<CacheStatsDto.QueryListResponse> getQueries(
            @RequestParam(required = false) String tableName,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.debug("Getting query list: tableName={}, limit={}", tableName, limit);
            CacheStatsDto.QueryListResponse response = cacheStatsService.getQueries(tableName, limit);
            return ApiResponse.success(response, "获取查询列表成功");
        } catch (Exception e) {
            log.error("Failed to get query list", e);
            return ApiResponse.error("QUERY_LIST_ERROR", "获取查询列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取特定查询的缓存规则
     * GET /api/cache/queries/{queryId}/rules
     */
    @GetMapping("/queries/{queryId}/rules")
    public ApiResponse<CacheStatsDto.CacheRuleListResponse> getQueryRules(@PathVariable String queryId) {
        try {
            log.debug("Getting cache rules for query: {}", queryId);
            List<CacheStatsDto.CacheRuleInfo> rules = cacheRuleService.getCacheRulesForQuery(queryId);
            CacheStatsDto.CacheRuleListResponse response = CacheStatsDto.CacheRuleListResponse.builder()
                .rules(rules)
                .totalCount((long) rules.size())
                .queryId(queryId)
                .build();
            return ApiResponse.success(response, "获取查询缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get query rules for: {}", queryId, e);
            return ApiResponse.error("QUERY_RULES_ERROR", "获取查询缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 为特定查询创建缓存规则
     * POST /api/cache/queries/{queryId}/rules
     */
    @PostMapping("/queries/{queryId}/rules")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> createQueryRule(
            @PathVariable String queryId, 
            @RequestBody CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            log.debug("Creating cache rule for query: {}", queryId);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.createCacheRuleForQuery(queryId, request);
            return ApiResponse.success(rule, "为查询创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create query rule for: {}", queryId, e);
            return ApiResponse.error("CREATE_QUERY_RULE_ERROR", "为查询创建缓存规则失败: " + e.getMessage());
        }
    }
    
    // ====================== 表格管理API ======================
    
    /**
     * 获取表格列表
     * GET /api/cache/tables
     */
    @GetMapping("/tables")
    public ApiResponse<CacheStatsDto.TableListResponse> getTables(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.debug("Getting table list: limit={}", limit);
            CacheStatsDto.TableListResponse response = cacheStatsService.getTables(limit);
            return ApiResponse.success(response, "获取表格列表成功");
        } catch (Exception e) {
            log.error("Failed to get table list", e);
            return ApiResponse.error("TABLE_LIST_ERROR", "获取表格列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取特定表格的缓存规则
     * GET /api/cache/tables/{tableName}/rules
     */
    @GetMapping("/tables/{tableName}/rules")
    public ApiResponse<CacheStatsDto.CacheRuleListResponse> getTableRules(@PathVariable String tableName) {
        try {
            log.debug("Getting cache rules for table: {}", tableName);
            List<CacheStatsDto.CacheRuleInfo> rules = cacheRuleService.getCacheRulesForTable(tableName);
            CacheStatsDto.CacheRuleListResponse response = CacheStatsDto.CacheRuleListResponse.builder()
                .rules(rules)
                .totalCount((long) rules.size())
                .tableName(tableName)
                .build();
            return ApiResponse.success(response, "获取表格缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get table rules for: {}", tableName, e);
            return ApiResponse.error("TABLE_RULES_ERROR", "获取表格缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 为特定表格创建缓存规则
     * POST /api/cache/tables/{tableName}/rules
     */
    @PostMapping("/tables/{tableName}/rules")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> createTableRule(
            @PathVariable String tableName, 
            @RequestBody CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            log.debug("Creating cache rule for table: {}", tableName);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.createCacheRuleForTable(tableName, request);
            return ApiResponse.success(rule, "为表格创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create table rule for: {}", tableName, e);
            return ApiResponse.error("CREATE_TABLE_RULE_ERROR", "为表格创建缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表格统计信息
     * GET /api/cache/tables/{tableName}/stats
     */
    @GetMapping("/tables/{tableName}/stats")
    public ApiResponse<CacheStatsDto.TableStatsInfo> getTableStats(@PathVariable String tableName) {
        try {
            log.debug("Getting table stats for: {}", tableName);
            CacheStatsDto.TableStatsInfo stats = cacheStatsService.getTableStats(tableName);
            return ApiResponse.success(stats, "获取表格统计信息成功");
        } catch (Exception e) {
            log.error("Failed to get table stats for: {}", tableName, e);
            return ApiResponse.error("TABLE_STATS_ERROR", "获取表格统计信息失败: " + e.getMessage());
        }
    }
    
    // ====================== 缓存规则管理API ======================
    
    /**
     * 获取所有缓存规则
     * GET /api/cache/rules
     */
    @GetMapping("/rules")
    public ApiResponse<CacheStatsDto.CacheRuleListResponse> getAllRules() {
        try {
            log.debug("Getting all cache rules");
            List<CacheStatsDto.CacheRuleInfo> rules = cacheRuleService.getAllRules();
            CacheStatsDto.CacheRuleListResponse response = CacheStatsDto.CacheRuleListResponse.builder()
                .rules(rules)
                .totalCount((long) rules.size())
                .build();
            return ApiResponse.success(response, "获取所有缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get all cache rules", e);
            return ApiResponse.error("GET_ALL_RULES_ERROR", "获取所有缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取缓存规则
     * GET /api/cache/rules/{ruleId}
     */
    @GetMapping("/rules/{ruleId}")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> getRuleById(@PathVariable String ruleId) {
        try {
            log.debug("Getting cache rule by ID: {}", ruleId);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.getRuleById(ruleId);
            if (rule == null) {
                return ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + ruleId);
            }
            return ApiResponse.success(rule, "获取缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to get cache rule by ID: {}", ruleId, e);
            return ApiResponse.error("GET_RULE_ERROR", "获取缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建缓存规则
     * POST /api/cache/rules
     */
    @PostMapping("/rules")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> createRule(@RequestBody CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            log.debug("Creating cache rule: {}", request);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.createRule(request);
            return ApiResponse.success(rule, "创建缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to create cache rule", e);
            return ApiResponse.error("CREATE_RULE_ERROR", "创建缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新缓存规则
     * PUT /api/cache/rules/{ruleId}
     */
    @PutMapping("/rules/{ruleId}")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> updateRule(
            @PathVariable String ruleId, 
            @RequestBody CacheStatsDto.CreateCacheRuleRequest request) {
        try {
            log.debug("Updating cache rule: {}", ruleId);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.updateRule(ruleId, request);
            return ApiResponse.success(rule, "更新缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to update cache rule: {}", ruleId, e);
            return ApiResponse.error("UPDATE_RULE_ERROR", "更新缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除缓存规则
     * DELETE /api/cache/rules/{ruleId}
     */
    @DeleteMapping("/rules/{ruleId}")
    public ApiResponse<String> deleteRule(@PathVariable String ruleId) {
        try {
            log.debug("Deleting cache rule: {}", ruleId);
            boolean success = cacheRuleService.deleteRule(ruleId);
            if (success) {
                return ApiResponse.success("Rule deleted successfully", "删除缓存规则成功");
            } else {
                return ApiResponse.error("RULE_NOT_FOUND", "缓存规则不存在: " + ruleId);
            }
        } catch (Exception e) {
            log.error("Failed to delete cache rule: {}", ruleId, e);
            return ApiResponse.error("DELETE_RULE_ERROR", "删除缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用缓存规则
     * POST /api/cache/rules/{ruleId}/enable
     */
    @PostMapping("/rules/{ruleId}/enable")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> enableRule(@PathVariable String ruleId) {
        try {
            log.debug("Enabling cache rule: {}", ruleId);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.toggleRule(ruleId, true);
            return ApiResponse.success(rule, "启用缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to enable cache rule: {}", ruleId, e);
            return ApiResponse.error("ENABLE_RULE_ERROR", "启用缓存规则失败: " + e.getMessage());
        }
    }
    
    /**
     * 禁用缓存规则
     * POST /api/cache/rules/{ruleId}/disable
     */
    @PostMapping("/rules/{ruleId}/disable")
    public ApiResponse<CacheStatsDto.CacheRuleInfo> disableRule(@PathVariable String ruleId) {
        try {
            log.debug("Disabling cache rule: {}", ruleId);
            CacheStatsDto.CacheRuleInfo rule = cacheRuleService.toggleRule(ruleId, false);
            return ApiResponse.success(rule, "禁用缓存规则成功");
        } catch (Exception e) {
            log.error("Failed to disable cache rule: {}", ruleId, e);
            return ApiResponse.error("DISABLE_RULE_ERROR", "禁用缓存规则失败: " + e.getMessage());
        }
    }
    
    // ====================== 测试和调试API ======================
    
    /**
     * 记录查询统计信息（用于测试和模拟数据）
     * POST /api/cache/record-query
     */
    @PostMapping("/record-query")
    public ApiResponse<String> recordQuery(@RequestParam String tableName,
                                         @RequestParam(defaultValue = "false") boolean isCached,
                                         @RequestParam(defaultValue = "false") boolean isHit,
                                         @RequestParam(defaultValue = "100.0") double executionTime) {
        try {
            log.debug("Recording query: table={}, cached={}, hit={}, time={}", 
                     tableName, isCached, isHit, executionTime);
            cacheStatsService.recordQuery(tableName, isCached, isHit, executionTime);
            return ApiResponse.success("Query recorded successfully", "记录查询统计信息成功");
        } catch (Exception e) {
            log.error("Failed to record query", e);
            return ApiResponse.error("RECORD_QUERY_ERROR", "记录查询统计信息失败: " + e.getMessage());
        }
    }
}
