package org.openjdbcproxy.cache.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.CacheOverviewStats;
import org.openjdbcproxy.cache.dto.HitRateStats;
import org.openjdbcproxy.cache.dto.PerformanceStats;
import org.openjdbcproxy.cache.service.CacheStatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存统计控制器
 * 提供缓存统计相关的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/stats")
@RequiredArgsConstructor
public class CacheStatsController {
    
    private final CacheStatsService cacheStatsService;
    
    /**
     * 获取缓存概览统计
     */
    @GetMapping("/overview")
    public CacheOverviewStats getOverviewStats() {
        log.info("获取缓存概览统计");
        return cacheStatsService.getOverviewStats();
    }
    
    /**
     * 获取缓存命中率统计
     * 
     * @param period 统计周期 (hour, day, week, month)，默认为day
     * @param datasource 数据源名称，用于过滤特定数据源的统计
     */
    @GetMapping("/hit-rate")
    public HitRateStats getHitRateStats(
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "datasource", required = false) String datasource) {
        log.info("获取缓存命中率统计，周期: {}, 数据源: {}", period, datasource);
        return cacheStatsService.getHitRateStats(period, datasource);
    }
    
    /**
     * 获取性能统计
     * 
     * @param period 统计周期 (hour, day, week)，默认为day
     * @param metric 指标类型 (response_time, throughput, cache_size)，默认为all
     */
    @GetMapping("/performance")
    public PerformanceStats getPerformanceStats(
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "metric", defaultValue = "all") String metric) {
        log.info("获取性能统计，周期: {}, 指标: {}", period, metric);
        return cacheStatsService.getPerformanceStats(period, metric);
    }
    
    /**
     * 获取慢查询统计
     * 
     * @param limit 返回记录数限制，默认为10
     * @param datasource 数据源名称，用于过滤特定数据源
     */
    @GetMapping("/slow_queries")
    public List<Map<String, Object>> getSlowQueries(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "datasource", required = false) String datasource) {
        log.info("获取慢查询统计，限制: {}, 数据源: {}", limit, datasource);
        return cacheStatsService.getSlowQueries(limit, datasource);
    }
    
    /**
     * 获取热门表格统计
     * 
     * @param limit 返回记录数限制，默认为10
     * @param datasource 数据源名称，用于过滤特定数据源
     */
    @GetMapping("/top-tables")
    public List<Map<String, Object>> getTopTables(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "datasource", required = false) String datasource) {
        log.info("获取热门表格统计，限制: {}, 数据源: {}", limit, datasource);
        return cacheStatsService.getTopTables(limit, datasource);
    }
    
    /**
     * 获取表格列表
     * 
     * @param limit 返回记录数限制，默认为10
     * @param datasource 数据源名称，用于过滤特定数据源
     */
    @GetMapping("/summary")
    public List<Map<String, Object>> getTablesList(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "datasource", required = false) String datasource) {
        log.info("获取表格列表，限制: {}, 数据源: {}", limit, datasource);
        return cacheStatsService.getTablesList(limit, datasource);
    }
}