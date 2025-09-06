package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.entity.CacheDecision;
import org.openjdbcproxy.cache.entity.Query;

/**
 * 缓存决策服务接口
 * 根据传入的SQL语句和相关规则，决策是否需要走缓存
 */
public interface CacheDecisionService {

    /**
     * 根据查询信息决策是否使用缓存
     * 
     * @param datasourceName 数据源名称
     * @param query 查询对象
     * @return 缓存决策结果
     */
    CacheDecision makeDecision(String datasourceName, Query query);

    /**
     * 根据SQL语句决策是否使用缓存（简化版本）
     * 
     * @param datasourceName 数据源名称
     * @param sql SQL语句
     * @return 缓存决策结果
     */
    CacheDecision makeDecision(String datasourceName, String sql);

    /**
     * 检查指定表是否有缓存规则
     * 
     * @param datasourceName 数据源名称
     * @param tableName 表名
     * @return 是否有缓存规则
     */
    boolean hasTableCacheRule(String datasourceName, String tableName);

    /**
     * 获取查询的TTL（生存时间）
     * 
     * @param datasourceName 数据源名称
     * @param query 查询对象
     * @return TTL秒数，0表示不缓存
     */
    int getQueryTtl(String datasourceName, Query query);

    /**
     * 刷新缓存规则（从Redis重新加载）
     * 
     * @param datasourceName 数据源名称
     */
    void refreshCacheRules(String datasourceName);
}