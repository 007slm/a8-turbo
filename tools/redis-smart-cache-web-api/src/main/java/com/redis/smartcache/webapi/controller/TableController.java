package com.redis.smartcache.webapi.controller;

import com.redis.smartcache.webapi.model.ApiResponse;
import com.redis.smartcache.webapi.model.RuleInfo;
import com.redis.smartcache.webapi.model.TableInfo;
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
 * 表格管理控制器
 * 提供表格列表、统计信息、规则创建等功能
 */
@RestController
@RequestMapping("/api/tables")
@Tag(name = "表格管理", description = "表格相关的API接口")
@CrossOrigin(origins = "*")
public class TableController {
    
    private static final Logger logger = LoggerFactory.getLogger(TableController.class);
    
    @Autowired
    private RedisSmartCacheService redisService;

    /**
     * 获取表格列表
     */
    @GetMapping
    @Operation(summary = "获取表格列表", description = "获取所有表格信息，支持排序")
    public ResponseEntity<ApiResponse<List<TableInfo>>> getTables(
            @Parameter(description = "排序字段 (accessFrequency, queryTime, name)")
            @RequestParam(required = false) String sortBy,
            
            @Parameter(description = "排序方向 (ASC, DESC)")
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {
        
        try {
            List<TableInfo> tables = redisService.getTables(sortBy, sortDirection);
            return ResponseEntity.ok(ApiResponse.success("获取表格列表成功", tables));
        } catch (Exception e) {
            logger.error("获取表格列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取表格列表失败: " + e.getMessage()));
        }
    }

    /**
     * 为表格创建缓存规则
     */
    @PostMapping("/create-rule")
    @Operation(summary = "创建表格规则", description = "为指定表格创建缓存规则")
    public ResponseEntity<ApiResponse<RuleInfo>> createTableRule(
            @RequestBody Map<String, String> request) {
        
        try {
            String tableName = request.get("tableName");
            String ttl = request.get("ttl");
            
            if (tableName == null || tableName.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("表格名称不能为空"));
            }
            if (ttl == null || ttl.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("TTL不能为空"));
            }
            
            RuleInfo rule = redisService.createTableRule(tableName, ttl);
            return ResponseEntity.ok(ApiResponse.success("创建表格规则成功", rule));
            
        } catch (Exception e) {
            logger.error("创建表格规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("创建表格规则失败: " + e.getMessage()));
        }
    }

    /**
     * 获取表格统计信息
     */
    @GetMapping("/{tableName}/stats")
    @Operation(summary = "获取表格统计", description = "获取指定表格的详细统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTableStats(
            @Parameter(description = "表格名称")
            @PathVariable String tableName) {
        
        try {
            Map<String, Object> stats = redisService.getTableStats(tableName);
            return ResponseEntity.ok(ApiResponse.success("获取表格统计成功", stats));
        } catch (Exception e) {
            logger.error("获取表格统计失败: " + tableName, e);
            return ResponseEntity.ok(ApiResponse.error("获取表格统计失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索表格
     */
    @GetMapping("/search")
    @Operation(summary = "搜索表格", description = "根据表格名称搜索")
    public ResponseEntity<ApiResponse<List<TableInfo>>> searchTables(
            @Parameter(description = "搜索关键词")
            @RequestParam String keyword,
            
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        
        try {
            List<TableInfo> allTables = redisService.getTables(null, null);
            
            List<TableInfo> filteredTables = allTables.stream()
                .filter(table -> table.getName().toLowerCase().contains(keyword.toLowerCase()))
                .limit(limit)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("搜索表格成功", filteredTables));
            
        } catch (Exception e) {
            logger.error("搜索表格失败", e);
            return ResponseEntity.ok(ApiResponse.error("搜索表格失败: " + e.getMessage()));
        }
    }

    /**
     * 获取表格概要统计
     */
    @GetMapping("/summary")
    @Operation(summary = "获取表格概要", description = "获取所有表格的概要统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTablesSummary() {
        try {
            List<TableInfo> tables = redisService.getTables(null, null);
            
            Map<String, Object> summary = Map.of(
                "totalTables", tables.size(),
                "tablesWithRules", tables.stream().mapToLong(t -> t.getRule() != null ? 1 : 0).sum(),
                "totalAccessFrequency", tables.stream().mapToLong(TableInfo::getAccessFrequency).sum(),
                "avgQueryTime", tables.stream().mapToDouble(TableInfo::getQueryTime).average().orElse(0.0),
                "maxQueryTime", tables.stream().mapToDouble(TableInfo::getQueryTime).max().orElse(0.0),
                "minQueryTime", tables.stream().mapToDouble(TableInfo::getQueryTime).min().orElse(0.0)
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取表格概要成功", summary));
            
        } catch (Exception e) {
            logger.error("获取表格概要失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取表格概要失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门表格
     */
    @GetMapping("/top")
    @Operation(summary = "获取热门表格", description = "获取访问频率最高的表格")
    public ResponseEntity<ApiResponse<List<TableInfo>>> getTopTables(
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        try {
            List<TableInfo> tables = redisService.getTables("accessFrequency", "DESC");
            
            List<TableInfo> topTables = tables.stream()
                .limit(limit)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("获取热门表格成功", topTables));
            
        } catch (Exception e) {
            logger.error("获取热门表格失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取热门表格失败: " + e.getMessage()));
        }
    }

    /**
     * 获取慢查询表格
     */
    @GetMapping("/slow")
    @Operation(summary = "获取慢查询表格", description = "获取查询时间最长的表格")
    public ResponseEntity<ApiResponse<List<TableInfo>>> getSlowTables(
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            
            @Parameter(description = "最小查询时间阈值（毫秒）")
            @RequestParam(required = false, defaultValue = "100.0") Double minTime) {
        
        try {
            List<TableInfo> tables = redisService.getTables("queryTime", "DESC");
            
            List<TableInfo> slowTables = tables.stream()
                .filter(table -> table.getQueryTime() >= minTime)
                .limit(limit)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("获取慢查询表格成功", slowTables));
            
        } catch (Exception e) {
            logger.error("获取慢查询表格失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取慢查询表格失败: " + e.getMessage()));
        }
    }

    /**
     * 批量为表格创建规则
     */
    @PostMapping("/batch-create-rules")
    @Operation(summary = "批量创建表格规则", description = "为多个表格批量创建缓存规则")
    public ResponseEntity<ApiResponse<List<RuleInfo>>> batchCreateTableRules(
            @RequestBody Map<String, Object> request) {
        
        try {
            @SuppressWarnings("unchecked")
            List<String> tableNames = (List<String>) request.get("tableNames");
            String ttl = (String) request.get("ttl");
            
            if (tableNames == null || tableNames.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("表格名称列表不能为空"));
            }
            if (ttl == null || ttl.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("TTL不能为空"));
            }
            
            List<RuleInfo> rules = tableNames.stream()
                .map(tableName -> {
                    try {
                        return redisService.createTableRule(tableName, ttl);
                    } catch (Exception e) {
                        logger.error("为表格创建规则失败: " + tableName, e);
                        return null;
                    }
                })
                .filter(rule -> rule != null)
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("批量创建表格规则成功", rules));
            
        } catch (Exception e) {
            logger.error("批量创建表格规则失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量创建表格规则失败: " + e.getMessage()));
        }
    }
}