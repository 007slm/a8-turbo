package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.dto.CacheRuleResponse;
import org.openjdbcproxy.cache.dto.CreateCacheRuleRequest;
import org.openjdbcproxy.cache.dto.UpdateCacheRuleRequest;

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
    Map<String, List<CacheRuleResponse>> getRules();

    /**
     * 创建缓存规则
     * 
     * @param request 创建请求
     * @return 创建的规则
     */
    CacheRuleResponse createRule(CreateCacheRuleRequest request);

    /**
     * 更新缓存规则
     * 
     * @param ruleId 规则ID
     * @param request 更新请求
     * @return 更新后的规则
     */
    CacheRuleResponse updateRule(String ruleId, UpdateCacheRuleRequest request);

    /**
     * 删除缓存规则
     * 
     * @param ruleId 规则ID
     */
    void deleteRule(String ruleId);

    /**
     * 获取指定数据源的所有缓存规则（运行时调用）
     * 
     * @param datasourceName 数据源名称
     * @return 缓存规则列表
     */
    List<CacheRule> getCacheRulesByDatasource(String datasourceName);

    /**
     * 根据查询匹配缓存规则（运行时调用）
     * 
     * @param datasourceName 数据源名称
     * @param sql SQL语句
     * @param tables 涉及的表名列表
     * @return 匹配的缓存规则，如果没有匹配则返回null
     */
    CacheRule matchCacheRule(String datasourceName, String sql, List<String> tables);
}