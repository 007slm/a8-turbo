package com.redis.smartcache.webapi.controller;

import com.redis.smartcache.webapi.model.ApiResponse;
import com.redis.smartcache.webapi.model.QueryInfo;
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
 * 查询管理控制器
 * 提供查询列表、详情查看、规则创建等功能
 */
@RestController
@RequestMapping("/api/queries")
@Tag(name = "查询管理", description = "查询相关的API接口")
@CrossOrigin(origins = "*")
public class QueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    
    @Autowired
    private RedisSmartCacheService redisService;

    /**
     * 获取查询列表
     */
    @GetMapping
    @Operation(summary = "获取查询列表", description = "获取所有查询信息，支持排序和分页")
    public ResponseEntity<ApiResponse<List<QueryInfo>>> getQueries(
            @Parameter(description = "排序字段 (queryTime, accessFrequency, tables, id)")
            @RequestParam(required = false) String sortBy,
            
            @Parameter(description = "排序方向 (ASC, DESC)")
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false) Integer limit,
            
            @Parameter(description = "偏移量")
            @RequestParam(required = false) Integer offset) {
        
        try {
            List<QueryInfo> queries = redisService.getQueries(sortBy, sortDirection, limit, offset);
            return ResponseEntity.ok(ApiResponse.success("获取查询列表成功", queries));
        } catch (Exception e) {
            logger.error("获取查询列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取查询列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取查询详情
     */
    @GetMapping("/{queryId}")
    @Operation(summary = "获取查询详情", description = "根据查询ID获取详细信息")
    public ResponseEntity<ApiResponse<QueryInfo>> getQueryById(
            @Parameter(description = "查询ID")
            @PathVariable String queryId) {
        
        try {
            QueryInfo query = redisService.getQueryById(queryId);
            if (query != null) {
                return ResponseEntity.ok(ApiResponse.success("获取查询详情成功", query));
            } else {
                return ResponseEntity.ok(ApiResponse.error("查询不存在: " + queryId));
            }
        } catch (Exception e) {
            logger.error("获取查询详情失败: " + queryId, e);
            return ResponseEntity.ok(ApiResponse.error("获取查询详情失败: " + e.getMessage()));
        }
    }

    /**
     * 为查询创建缓存规则
     */
    @PostMapping("/create-rule")
    @Operation(summary = "创建查询规则", description = "为指定查询创建缓存规则")
    public ResponseEntity<ApiResponse<RuleInfo>> createQueryRule(
            @RequestBody Map<String, String> request) {
        
        try {
            String queryId = request.get("queryId");
            String ttl = request.get("ttl");
            
            if (queryId == null || queryId.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("查询ID不能为空"));
            }
            if (ttl == null || ttl.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("TTL不能为空"));
            }
            
            RuleInfo rule = redisService.createQueryRule(queryId, ttl);
            return ResponseEntity.ok(ApiResponse.success("创建查询规则成功", rule));
            
        } catch (Exception e) {
            logger.error("创建查询规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("创建查询规则失败: " + e.getMessage()));
        }
    }

    /**
     * 批量获取查询信息（用于前端批量操作）
     */
    @PostMapping("/batch")
    @Operation(summary = "批量获取查询", description = "根据查询ID列表批量获取查询信息")
    public ResponseEntity<ApiResponse<List<QueryInfo>>> getQueriesBatch(
            @RequestBody Map<String, List<String>> request) {
        
        try {
            List<String> queryIds = request.get("queryIds");
            if (queryIds == null || queryIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("查询ID列表不能为空"));
            }
            
            List<QueryInfo> queries = queryIds.stream()
                .map(redisService::getQueryById)
                .filter(query -> query != null)
                .toList();
                
            return ResponseEntity.ok(ApiResponse.success("批量获取查询成功", queries));
            
        } catch (Exception e) {
            logger.error("批量获取查询失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量获取查询失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索查询（基于SQL内容或表名）
     */
    @GetMapping("/search")
    @Operation(summary = "搜索查询", description = "根据关键词搜索查询")
    public ResponseEntity<ApiResponse<List<QueryInfo>>> searchQueries(
            @Parameter(description = "搜索关键词")
            @RequestParam String keyword,
            
            @Parameter(description = "搜索类型 (sql, table, all)")
            @RequestParam(required = false, defaultValue = "all") String searchType,
            
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        
        try {
            // 获取所有查询然后过滤
            List<QueryInfo> allQueries = redisService.getQueries(null, null, null, null);
            
            List<QueryInfo> filteredQueries = allQueries.stream()
                .filter(query -> {
                    if ("sql".equals(searchType)) {
                        return query.getSql() != null && 
                               query.getSql().toLowerCase().contains(keyword.toLowerCase());
                    } else if ("table".equals(searchType)) {
                        return query.getTables() != null && 
                               query.getTables().stream().anyMatch(table -> 
                                   table.toLowerCase().contains(keyword.toLowerCase()));
                    } else {
                        // all - 搜索SQL和表名
                        boolean sqlMatch = query.getSql() != null && 
                                         query.getSql().toLowerCase().contains(keyword.toLowerCase());
                        boolean tableMatch = query.getTables() != null && 
                                           query.getTables().stream().anyMatch(table -> 
                                               table.toLowerCase().contains(keyword.toLowerCase()));
                        return sqlMatch || tableMatch;
                    }
                })
                .limit(limit)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("搜索查询成功", filteredQueries));
            
        } catch (Exception e) {
            logger.error("搜索查询失败", e);
            return ResponseEntity.ok(ApiResponse.error("搜索查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取查询统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取查询统计", description = "获取查询的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQueryStats() {
        try {
            List<QueryInfo> queries = redisService.getQueries(null, null, null, null);
            
            Map<String, Object> stats = Map.of(
                "totalQueries", queries.size(),
                "cachedQueries", queries.stream().mapToLong(q -> q.isCached() ? 1 : 0).sum(),
                "avgQueryTime", queries.stream().mapToDouble(QueryInfo::getMeanTime).average().orElse(0.0),
                "maxQueryTime", queries.stream().mapToDouble(QueryInfo::getMeanTime).max().orElse(0.0),
                "totalExecutions", queries.stream().mapToLong(QueryInfo::getCount).sum()
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取查询统计成功", stats));
            
        } catch (Exception e) {
            logger.error("获取查询统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取查询统计失败: " + e.getMessage()));
        }
    }
}