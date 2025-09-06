package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.dto.QueryDetailResponse;
import org.openjdbcproxy.cache.dto.QuerySummary;

import java.util.List;
import java.util.Map;

/**
 * 查询管理服务接口
 * 负责查询的注册、统计、管理等功能
 */
public interface QueryManagementService {

    /**
     * 获取查询列表（按数据库分组）
     * 
     * @return 按数据库名称分组的查询列表
     */
    Map<String, List<QuerySummary>> getQueryList();

    /**
     * 获取查询详情
     * 
     * @param queryId 查询ID
     * @return 查询详情，不存在时返回null
     */
    QueryDetailResponse getQueryDetail(String queryId);
}