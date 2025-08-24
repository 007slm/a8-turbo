package com.redis.smartcache.webapi.service;

import com.redis.smartcache.webapi.model.QueryInfo;
import com.redis.smartcache.webapi.model.RuleInfo;
import com.redis.smartcache.webapi.model.StatsModels;
import com.redis.smartcache.webapi.model.TableInfo;

import java.util.List;
import java.util.Map;

/**
 * Redis Smart Cache 服务接口
 * 定义所有与Redis智能缓存相关的核心业务方法
 */
public interface RedisSmartCacheService {
    
    // ====================== 连接管理 ======================
    
    /**
     * 测试Redis连接
     * @return 连接测试结果
     */
    boolean testConnection();
    
    /**
     * Ping Redis服务器
     * @return ping响应
     */
    String ping();
    
    /**
     * 获取连接状态信息
     * @return 连接状态详情
     */
    Map<String, Object> getConnectionStatus();

    // ====================== 查询管理 ======================
    
    /**
     * 获取所有查询信息
     * @param sortBy 排序字段 (可选: queryTime, accessFrequency, tables, id)
     * @param sortDirection 排序方向 (ASC/DESC)
     * @param limit 限制返回数量
     * @param offset 偏移量
     * @return 查询信息列表
     */
    List<QueryInfo> getQueries(String sortBy, String sortDirection, Integer limit, Integer offset);
    
    /**
     * 根据ID获取查询详情
     * @param queryId 查询ID
     * @return 查询详情
     */
    QueryInfo getQueryById(String queryId);
    
    /**
     * 为查询创建缓存规则
     * @param queryId 查询ID
     * @param ttl 缓存时间
     * @return 创建的规则信息
     */
    RuleInfo createQueryRule(String queryId, String ttl);

    // ====================== 表格管理 ======================
    
    /**
     * 获取所有表格信息
     * @param sortBy 排序字段 (可选: accessFrequency, queryTime)
     * @param sortDirection 排序方向 (ASC/DESC)
     * @return 表格信息列表
     */
    List<TableInfo> getTables(String sortBy, String sortDirection);
    
    /**
     * 为表格创建缓存规则
     * @param tableName 表格名称
     * @param ttl 缓存时间
     * @return 创建的规则信息
     */
    RuleInfo createTableRule(String tableName, String ttl);
    
    /**
     * 获取表格统计信息
     * @param tableName 表格名称
     * @return 表格统计信息
     */
    Map<String, Object> getTableStats(String tableName);

    // ====================== 规则管理 ======================
    
    /**
     * 获取所有缓存规则
     * @return 规则列表
     */
    List<RuleInfo> getRules();
    
    /**
     * 创建新的缓存规则
     * @param ruleInfo 规则信息
     * @return 创建的规则信息
     */
    RuleInfo createRule(RuleInfo ruleInfo);
    
    /**
     * 更新缓存规则
     * @param ruleId 规则ID
     * @param ruleInfo 更新的规则信息
     * @return 更新后的规则信息
     */
    RuleInfo updateRule(String ruleId, RuleInfo ruleInfo);
    
    /**
     * 删除缓存规则
     * @param ruleId 规则ID
     * @return 是否删除成功
     */
    boolean deleteRule(String ruleId);
    
    /**
     * 批量提交规则
     * @param rules 规则列表
     * @return 提交结果
     */
    boolean commitRules(List<RuleInfo> rules);
    
    /**
     * 验证规则配置
     * @param ruleInfo 待验证的规则
     * @return 验证结果和错误信息
     */
    Map<String, Object> validateRule(RuleInfo ruleInfo);

    // ====================== 统计信息 ======================
    
    /**
     * 获取总体统计信息
     * @return 总体统计
     */
    StatsModels.OverviewStats getOverviewStats();
    
    /**
     * 获取缓存命中率统计
     * @param timeRange 时间范围 (例如: 24h, 7d, 30d)
     * @return 缓存命中率统计
     */
    StatsModels.CacheHitStats getCacheHitStats(String timeRange);
    
    /**
     * 获取查询性能统计
     * @param timeRange 时间范围
     * @return 查询性能统计
     */
    StatsModels.QueryPerformanceStats getQueryPerformanceStats(String timeRange);
    
    /**
     * 获取热门表格统计
     * @param limit 返回数量限制
     * @return 热门表格列表
     */
    List<StatsModels.TopTable> getTopTablesStats(Integer limit);
    
    /**
     * 获取慢查询统计
     * @param limit 返回数量限制
     * @param minTime 最小执行时间阈值
     * @return 慢查询列表
     */
    List<StatsModels.SlowQuery> getSlowQueriesStats(Integer limit, Double minTime);

    // ====================== 配置管理 ======================
    
    /**
     * 获取应用配置
     * @return 配置信息
     */
    Map<String, Object> getConfig();
    
    /**
     * 更新应用配置
     * @param config 新的配置
     * @return 是否更新成功
     */
    boolean updateConfig(Map<String, Object> config);
    
    /**
     * 重置配置到默认值
     * @return 是否重置成功
     */
    boolean resetConfig();

    // ====================== 其他工具方法 ======================
    
    /**
     * 检查Smart Cache索引是否存在
     * @return 是否存在
     */
    boolean checkSmartCacheIndex();
    
    /**
     * 获取应用名称
     * @return 当前应用名称
     */
    String getApplicationName();
}