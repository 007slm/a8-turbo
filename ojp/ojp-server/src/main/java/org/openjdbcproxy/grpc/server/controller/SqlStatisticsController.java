package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.SqlStatisticsResponse;
import org.openjdbcproxy.grpc.server.smartcache.statistics.SqlStatisticsData;
import org.openjdbcproxy.grpc.server.smartcache.statistics.TableStatisticsData;
import org.openjdbcproxy.grpc.server.smartcache.statistics.service.RedisStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL统计数据控制器
 * 提供统计数据的查询和管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class SqlStatisticsController {
    
    @Autowired
    private RedisStatisticsService redisStatisticsService;
    
    /**
     * 获取所有SQL统计数据
     */
    @GetMapping("/sql")
    public ResponseEntity<SqlStatisticsResponse> getAllSqlStatistics() {
        try {
            List<SqlStatisticsData> statistics = redisStatisticsService.getAllSqlStatistics();
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取SQL统计数据成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取SQL统计数据失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取SQL统计数据失败: " + e.getMessage())
                    .errorCode("SQL_STATISTICS_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取指定SQL统计数据
     */
    @GetMapping("/sql/{queryId}")
    public ResponseEntity<SqlStatisticsResponse> getSqlStatistics(@PathVariable String queryId) {
        try {
            SqlStatisticsData statistics = redisStatisticsService.getSqlStatistics(queryId);
            
            if (statistics == null) {
                SqlStatisticsResponse notFoundResponse = SqlStatisticsResponse.builder()
                        .success(false)
                        .message("SQL统计数据不存在: " + queryId)
                        .errorCode("SQL_STATISTICS_NOT_FOUND")
                        .timestamp(System.currentTimeMillis())
                        .build();
                return ResponseEntity.status(404).body(notFoundResponse);
            }
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取SQL统计数据成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取SQL统计数据失败: {}", queryId, e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取SQL统计数据失败: " + e.getMessage())
                    .errorCode("SQL_STATISTICS_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取热门SQL查询
     */
    @GetMapping("/sql/hot")
    public ResponseEntity<SqlStatisticsResponse> getHotSqlQueries(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<SqlStatisticsData> statistics = redisStatisticsService.getHotSqlQueries(limit);
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取热门SQL查询成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取热门SQL查询失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取热门SQL查询失败: " + e.getMessage())
                    .errorCode("HOT_SQL_QUERIES_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取慢查询
     */
    @GetMapping("/sql/slow")
    public ResponseEntity<SqlStatisticsResponse> getSlowQueries(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<SqlStatisticsData> statistics = redisStatisticsService.getSlowQueries(limit);
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取慢查询成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取慢查询失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取慢查询失败: " + e.getMessage())
                    .errorCode("SLOW_QUERIES_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取所有表统计数据
     */
    @GetMapping("/tables")
    public ResponseEntity<SqlStatisticsResponse> getAllTableStatistics() {
        try {
            List<TableStatisticsData> statistics = redisStatisticsService.getAllTableStatistics();
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取表统计数据成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取表统计数据失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取表统计数据失败: " + e.getMessage())
                    .errorCode("TABLE_STATISTICS_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取指定表统计数据
     */
    @GetMapping("/tables/{tableName}")
    public ResponseEntity<SqlStatisticsResponse> getTableStatistics(@PathVariable String tableName) {
        try {
            TableStatisticsData statistics = redisStatisticsService.getTableStatistics(tableName);
            
            if (statistics == null) {
                SqlStatisticsResponse notFoundResponse = SqlStatisticsResponse.builder()
                        .success(false)
                        .message("表统计数据不存在: " + tableName)
                        .errorCode("TABLE_STATISTICS_NOT_FOUND")
                        .timestamp(System.currentTimeMillis())
                        .build();
                return ResponseEntity.status(404).body(notFoundResponse);
            }
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取表统计数据成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取表统计数据失败: {}", tableName, e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取表统计数据失败: " + e.getMessage())
                    .errorCode("TABLE_STATISTICS_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取热门表
     */
    @GetMapping("/tables/hot")
    public ResponseEntity<SqlStatisticsResponse> getHotTables(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TableStatisticsData> statistics = redisStatisticsService.getHotTables(limit);
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取热门表成功")
                    .data(statistics)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取热门表失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取热门表失败: " + e.getMessage())
                    .errorCode("HOT_TABLES_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取缓存命中率统计
     */
    @GetMapping("/cache/hit-rate")
    public ResponseEntity<SqlStatisticsResponse> getCacheHitRateStats() {
        try {
            Map<String, Object> stats = redisStatisticsService.getCacheHitRateStats();
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取缓存命中率统计成功")
                    .data(stats)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取缓存命中率统计失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取缓存命中率统计失败: " + e.getMessage())
                    .errorCode("CACHE_HIT_RATE_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取统计概览
     */
    @GetMapping("/overview")
    public ResponseEntity<SqlStatisticsResponse> getStatisticsOverview() {
        try {
            List<SqlStatisticsData> sqlStats = redisStatisticsService.getAllSqlStatistics();
            List<TableStatisticsData> tableStats = redisStatisticsService.getAllTableStatistics();
            Map<String, Object> cacheStats = redisStatisticsService.getCacheHitRateStats();
            
            // 计算总体统计
            long totalSqlQueries = sqlStats.size();
            long totalTables = tableStats.size();
            long totalExecutions = sqlStats.stream().mapToLong(SqlStatisticsData::getExecutionCount).sum();
            double avgExecutionTime = sqlStats.stream()
                    .mapToDouble(SqlStatisticsData::getAverageExecutionTime)
                    .average()
                    .orElse(0.0);
            
            SqlStatisticsResponse.StatisticsOverview overview = SqlStatisticsResponse.StatisticsOverview.builder()
                    .totalQueries(totalSqlQueries)
                    .cacheHits((Long) cacheStats.getOrDefault("hits", 0L))
                    .cacheMisses((Long) cacheStats.getOrDefault("misses", 0L))
                    .hitRate((Double) cacheStats.getOrDefault("hitRate", 0.0))
                    .avgQueryTime(avgExecutionTime)
                    .slowQueries(sqlStats.stream().filter(s -> s.getAverageExecutionTime() > 1000).count())
                    .cacheStats(SqlStatisticsResponse.CacheStats.builder()
                            .totalCaches((Long) cacheStats.getOrDefault("totalCaches", 0L))
                            .activeCaches((Long) cacheStats.getOrDefault("activeCaches", 0L))
                            .memoryUsagePercent((Double) cacheStats.getOrDefault("memoryUsagePercent", 0.0))
                            .avgResponseTime((Double) cacheStats.getOrDefault("avgResponseTime", 0.0))
                            .build())
                    .build();
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("获取统计概览成功")
                    .data(overview)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取统计概览失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("获取统计概览失败: " + e.getMessage())
                    .errorCode("STATISTICS_OVERVIEW_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 清理过期统计数据
     */
    @PostMapping("/cleanup")
    public ResponseEntity<SqlStatisticsResponse> cleanupExpiredStatistics() {
        try {
            redisStatisticsService.cleanupExpiredStatistics();
            
            SqlStatisticsResponse response = SqlStatisticsResponse.builder()
                    .success(true)
                    .message("统计数据清理完成")
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清理过期统计数据失败", e);
            SqlStatisticsResponse errorResponse = SqlStatisticsResponse.builder()
                    .success(false)
                    .message("清理过期统计数据失败: " + e.getMessage())
                    .errorCode("CLEANUP_ERROR")
                    .timestamp(System.currentTimeMillis())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
