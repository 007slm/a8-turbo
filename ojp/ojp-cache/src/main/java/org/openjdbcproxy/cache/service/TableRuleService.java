package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.dto.CacheRuleResponse;
import org.openjdbcproxy.cache.dto.CreateTableRuleRequest;

import java.util.List;

/**
 * 表格缓存规则服务接口
 * 提供表格缓存规则的管理功能
 */
public interface TableRuleService {

    /**
     * 获取表格缓存规则
     * 
     * @param tableName 表名
     * @param datasource 数据源名称（可选）
     * @return 缓存规则列表
     */
    List<CacheRuleResponse> getTableRules(String tableName, String datasource);

    /**
     * 为表格创建缓存规则
     * 
     * @param tableName 表名
     * @param request 创建请求
     * @return 创建的缓存规则
     */
    CacheRuleResponse createTableRule(String tableName, CreateTableRuleRequest request);
}