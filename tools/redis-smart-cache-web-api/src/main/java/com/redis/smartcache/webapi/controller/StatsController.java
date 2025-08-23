package com.redis.smartcache.webapi.controller;

import com.redis.smartcache.webapi.model.ApiResponse;
import com.redis.smartcache.webapi.model.StatsModels;
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

/**
 * 统计和监控控制器
 * 提供各种统计信息和监控数据的API接口
 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计监控", description = "统计和监控相关的API接口")
@CrossOrigin(origins = "*")
public class StatsController {
    
    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);
    
    @Autowired
    private RedisSmartCacheService redisService;

    /**
     * 获取总体统计信息
     */
    @GetMapping("/overview")
    @Operation(summary = "获取总体统计", description = "获取系统的总体统计信息")
    public ResponseEntity<ApiResponse<StatsModels.OverviewStats>> getOverviewStats() {
        try {
            StatsModels.OverviewStats stats = redisService.getOverviewStats();
            return ResponseEntity.ok(ApiResponse.success("获取总体统计成功", stats));
        } catch (Exception e) {
            logger.error("获取总体统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取总体统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取缓存命中率统计
     */
    @GetMapping("/cache-hit")
    @Operation(summary = "获取缓存命中率", description = "获取指定时间范围内的缓存命中率统计")
    public ResponseEntity<ApiResponse<StatsModels.CacheHitStats>> getCacheHitStats(
            @Parameter(description = "时间范围 (1h, 6h, 24h, 7d, 30d)")
            @RequestParam(required = false, defaultValue = "24h") String timeRange) {
        
        try {
            StatsModels.CacheHitStats stats = redisService.getCacheHitStats(timeRange);
            return ResponseEntity.ok(ApiResponse.success("获取缓存命中率统计成功", stats));
        } catch (Exception e) {
            logger.error("获取缓存命中率统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取缓存命中率统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取查询性能统计
     */
    @GetMapping("/query-performance")
    @Operation(summary = "获取查询性能统计", description = "获取指定时间范围内的查询性能统计")
    public ResponseEntity<ApiResponse<StatsModels.QueryPerformanceStats>> getQueryPerformanceStats(
            @Parameter(description = "时间范围 (1h, 6h, 24h, 7d, 30d)")
            @RequestParam(required = false, defaultValue = "24h") String timeRange) {
        
        try {
            StatsModels.QueryPerformanceStats stats = redisService.getQueryPerformanceStats(timeRange);
            return ResponseEntity.ok(ApiResponse.success("获取查询性能统计成功", stats));
        } catch (Exception e) {
            logger.error("获取查询性能统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取查询性能统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门表格统计
     */
    @GetMapping("/top-tables")
    @Operation(summary = "获取热门表格", description = "获取访问频率最高的表格统计")
    public ResponseEntity<ApiResponse<List<StatsModels.TopTable>>> getTopTablesStats(
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        try {
            List<StatsModels.TopTable> stats = redisService.getTopTablesStats(limit);
            return ResponseEntity.ok(ApiResponse.success("获取热门表格统计成功", stats));
        } catch (Exception e) {
            logger.error("获取热门表格统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取热门表格统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取慢查询统计
     */
    @GetMapping("/slow-queries")
    @Operation(summary = "获取慢查询统计", description = "获取执行时间最长的查询统计")
    public ResponseEntity<ApiResponse<List<StatsModels.SlowQuery>>> getSlowQueriesStats(
            @Parameter(description = "返回数量限制")
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            
            @Parameter(description = "最小执行时间阈值（毫秒）")
            @RequestParam(required = false, defaultValue = "100.0") Double minTime) {
        
        try {
            List<StatsModels.SlowQuery> stats = redisService.getSlowQueriesStats(limit, minTime);
            return ResponseEntity.ok(ApiResponse.success("获取慢查询统计成功", stats));
        } catch (Exception e) {
            logger.error("获取慢查询统计失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取慢查询统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取实时指标
     */
    @GetMapping("/realtime")
    @Operation(summary = "获取实时指标", description = "获取当前的实时监控指标")
    public ResponseEntity<ApiResponse<Object>> getRealtimeStats() {
        try {
            // 组合多个统计信息提供实时概览
            StatsModels.OverviewStats overview = redisService.getOverviewStats();
            StatsModels.CacheHitStats cacheHit = redisService.getCacheHitStats("1h");
            List<StatsModels.TopTable> topTables = redisService.getTopTablesStats(5);
            
            Object realtimeStats = java.util.Map.of(
                "overview", overview,
                "cacheHit", cacheHit,
                "topTables", topTables,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取实时指标成功", realtimeStats));
        } catch (Exception e) {
            logger.error("获取实时指标失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取实时指标失败: " + e.getMessage()));
        }
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trends")
    @Operation(summary = "获取趋势数据", description = "获取指定时间范围内的趋势数据")
    public ResponseEntity<ApiResponse<Object>> getTrendsStats(
            @Parameter(description = "时间范围")
            @RequestParam(required = false, defaultValue = "24h") String timeRange,
            
            @Parameter(description = "数据点数量")
            @RequestParam(required = false, defaultValue = "24") Integer points) {
        
        try {
            // 这里应该实现真实的趋势数据查询
            // 由于时间序列数据需要额外的存储和查询逻辑，这里提供模拟数据
            
            List<StatsModels.TimeSeriesPoint> queryTimePoints = generateMockTimeSeries(points, 100.0, 50.0);
            List<StatsModels.TimeSeriesPoint> cacheHitPoints = generateMockTimeSeries(points, 80.0, 10.0);
            List<StatsModels.TimeSeriesPoint> throughputPoints = generateMockTimeSeries(points, 1000.0, 200.0);
            
            Object trendsData = java.util.Map.of(
                "timeRange", timeRange,
                "queryTime", queryTimePoints,
                "cacheHitRate", cacheHitPoints,
                "throughput", throughputPoints,
                "lastUpdated", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取趋势数据成功", trendsData));
        } catch (Exception e) {
            logger.error("获取趋势数据失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取趋势数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取性能对比数据
     */
    @GetMapping("/performance-comparison")
    @Operation(summary = "获取性能对比", description = "获取缓存前后的性能对比数据")
    public ResponseEntity<ApiResponse<Object>> getPerformanceComparison(
            @Parameter(description = "时间范围")
            @RequestParam(required = false, defaultValue = "24h") String timeRange) {
        
        try {
            StatsModels.OverviewStats overview = redisService.getOverviewStats();
            
            // 计算性能提升
            double performanceImprovement = overview.getAvgQueryTime() > 0 && overview.getAvgCachedQueryTime() > 0
                ? ((overview.getAvgQueryTime() - overview.getAvgCachedQueryTime()) / overview.getAvgQueryTime()) * 100
                : 0.0;
            
            Object comparisonData = java.util.Map.of(
                "timeRange", timeRange,
                "withoutCache", java.util.Map.of(
                    "avgQueryTime", overview.getAvgQueryTime(),
                    "totalQueries", overview.getTotalQueries()
                ),
                "withCache", java.util.Map.of(
                    "avgQueryTime", overview.getAvgCachedQueryTime(),
                    "cachedQueries", overview.getCachedQueries(),
                    "cacheHitRate", overview.getCacheHitRate()
                ),
                "improvement", java.util.Map.of(
                    "performanceGain", performanceImprovement,
                    "timeSaved", overview.getAvgQueryTime() - overview.getAvgCachedQueryTime(),
                    "cacheEfficiency", overview.getCacheHitRate()
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取性能对比数据成功", comparisonData));
        } catch (Exception e) {
            logger.error("获取性能对比数据失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取性能对比数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "获取系统健康状态", description = "获取系统各组件的健康状态")
    public ResponseEntity<ApiResponse<Object>> getSystemHealth() {
        try {
            boolean redisConnected = redisService.testConnection();
            boolean indexExists = redisService.checkSmartCacheIndex();
            
            String status = redisConnected && indexExists ? "healthy" : "unhealthy";
            
            Object healthData = java.util.Map.of(
                "status", status,
                "components", java.util.Map.of(
                    "redis", java.util.Map.of(
                        "status", redisConnected ? "up" : "down",
                        "description", redisConnected ? "Redis连接正常" : "Redis连接失败"
                    ),
                    "smartcache", java.util.Map.of(
                        "status", indexExists ? "up" : "down", 
                        "description", indexExists ? "Smart Cache索引存在" : "Smart Cache索引不存在"
                    )
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取系统健康状态成功", healthData));
        } catch (Exception e) {
            logger.error("获取系统健康状态失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取系统健康状态失败: " + e.getMessage()));
        }
    }

    /**
     * 生成模拟时间序列数据（用于演示）
     */
    private List<StatsModels.TimeSeriesPoint> generateMockTimeSeries(int points, double baseValue, double variance) {
        List<StatsModels.TimeSeriesPoint> series = new java.util.ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long interval = 3600000; // 1小时间隔
        
        for (int i = 0; i < points; i++) {
            long timestamp = currentTime - (points - i - 1) * interval;
            double value = baseValue + (Math.random() - 0.5) * variance;
            series.add(new StatsModels.TimeSeriesPoint(timestamp, Math.max(0, value)));
        }
        
        return series;
    }
}