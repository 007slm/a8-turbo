package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.entity.CacheDecision;
import org.openjdbcproxy.cache.entity.Query;

import java.util.List;

/**
 * 缓存决策服务接口
 * 根据传入的SQL语句和相关规则，决策是否需要走缓存
 */
public interface CacheDecisionService {

    /**
     * 根据连接哈希和查询对象做缓存决策
     * @param connHash 连接哈希值
     * @param query 查询对象
     * @return 缓存决策
     */
    CacheDecision makeDecision(String connHash, Query query);

    /**
     * 根据连接哈希和SQL语句做缓存决策
     * @param connHash 连接哈希值
     * @param sql SQL语句
     * @return 缓存决策
     */
    CacheDecision makeDecision(String connHash, String sql);

    /**
     * 检查表是否有缓存规则
     * @param connHash 连接哈希值
     * @param tableName 表名
     * @return 是否有缓存规则
     */
    boolean hasTableCacheRule(String connHash, String tableName);

    /**
     * 获取查询的TTL（生存时间）/**
     * 获取查询的TTL
     * @param connHash 连接哈希值
     * @param query 查询对象
     * @return TTL（秒）
     */
    int getQueryTtl(String connHash, Query query);

    /**
     * 刷新缓存规则（从Redis重新加载）/**
     * 刷新缓存规则
     * @param connHash 连接哈希值
     */
    void refreshCacheRules(String connHash);
    
    /**
     * 从SQL中提取表名
     * 
     * @param sql SQL语句
     * @return 表名列表
     */
    List<String> extractTables(String sql);
}