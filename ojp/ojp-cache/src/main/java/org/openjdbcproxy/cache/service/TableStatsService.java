package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.dto.TableStatsResponse;
import org.openjdbcproxy.cache.dto.TableDetailResponse;

import java.util.List;
import java.util.Map;

/**
 * 表格统计服务接口
 * 提供表格统计数据的聚合和计算功能
 */
public interface TableStatsService {

    /**
     * 获取表格统计列表（按数据库分组）
     * 
     * @return 按数据库名称分组的表格统计列表
     */
    Map<String, List<TableStatsResponse>> getTableStatsList();

    /**
     * 获取表格详细统计信息
     * 
     * @param tableName 表名
     * @param datasource 数据源名称（可选）
     * @return 表格详细统计信息
     */
    TableDetailResponse getTableDetails(String tableName, String datasource);
}