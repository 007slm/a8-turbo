package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.dto.CacheOverviewStats;
import org.openjdbcproxy.cache.dto.HitRateStats;
import org.openjdbcproxy.cache.dto.PerformanceStats;

import java.util.List;
import java.util.Map;

/**
 * 缓存统计服务接口
 * 提供各种缓存统计数据的聚合和计算功能
 */
public interface CacheStatsService {
    
    /**
     * 获取缓存概览统计
     */
    CacheOverviewStats getOverviewStats();
    
    /**
     * 获取缓存命中率统计
     * 
     * @param period 统计周期 (hour, day, week, month)
     * @param datasource 数据源名称，可选
     */
    HitRateStats getHitRateStats(String period, String datasource);
    
    /**
     * 获取性能统计
     * 
     * @param period 统计周期 (hour, day, week)
     * @param metric 指标类型 (response_time, throughput, cache_size)
     */
    PerformanceStats getPerformanceStats(String period, String metric);
    
    /**
     * 获取慢查询统计
     * 
     * @param limit 返回记录数限制
     * @param datasource 数据源名称，可选
     */
    List<Map<String, Object>> getSlowQueries(int limit, String datasource);
    
    /**
     * 获取热门表格统计
     * 
     * @param limit 返回记录数限制
     * @param datasource 数据源名称，可选
     */
    List<Map<String, Object>> getTopTables(int limit, String datasource);
    
    /**
     * 获取表格列表
     * 
     * @param limit 返回记录数限制
     * @param datasource 数据源名称，可选
     */
    List<Map<String, Object>> getTablesList(int limit, String datasource);
}