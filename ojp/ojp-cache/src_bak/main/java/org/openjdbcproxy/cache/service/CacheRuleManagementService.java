package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.CacheRule;

import java.util.List;
import java.util.Map;

/**
 * 缓存规则管理服务接口
 * 负责缓存规则的CRUD操作
 * 严格按照设计文档要求：去除分页参数，全量返回，按数据库分组
 */
public interface CacheRuleManagementService {

    /**
     * 获取缓存规则列表（按数据库分组）
     * 去除分页参数，缓存规则数量有限，全量返回
     * 
     * @return 按数据库名称分组的规则列表
     */
    Map<String, List<CacheRule>> getRules();

    /**
     * 创建缓存规则
     * 
     * @param request 创建请求
     * @return 创建的规则
     */
    CacheRule createRule(CacheRule request);

    /**
     * 更新缓存规则
     * 
     * @param ruleId 规则ID
     * @param request 更新请求
     * @return 更新后的规则
     */
    CacheRule updateRule(String ruleId, CacheRule request);

    /**
     * 删除缓存规则
     * 
     * @param ruleId 规则ID
     */
    void deleteRule(String ruleId);

    /**
     * 获取指定数据源的所有缓存规则（运行时调用）/**
     * 根据连接哈希获取缓存规则
     * @param connHash 连接哈希值
     * @return 缓存规则列表
     */
    List<CacheRule> getCacheRulesByConnHash(String connHash);

    /**
     * 根据查询匹配缓存规则（运行时调用）/**
     * 匹配缓存规则
     * @param connHash 连接哈希值
     * @param sql SQL语句
     * @param tables 表名列表
     * @return 匹配的缓存规则
     */
    CacheRule matchCacheRule(String connHash, String sql, List<String> tables);
}