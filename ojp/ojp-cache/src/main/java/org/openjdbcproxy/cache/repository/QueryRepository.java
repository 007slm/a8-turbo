package org.openjdbcproxy.cache.repository;

import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.dto.QuerySummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 查询数据存储接口
 * 负责查询信息的持久化和检索
 */
public interface QueryRepository {

    /**
     * 保存查询信息
     * @param query 查询对象
     * @return 保存后的查询对象
     */
    Query save(Query query);

    /**
     * 根据查询ID获取查询信息
     * @param queryId 查询ID
     * @return 查询对象（可能为空）
     */
    Optional<Query> findById(String queryId);

    /**
     * 根据SQL哈希值获取查询信息
     * @param sqlHash SQL哈希值
     * @param datasourceName 数据源名称
     * @return 查询对象（可能为空）
     */
    Optional<Query> findBySqlHash(String sqlHash, String datasourceName);

    /**
     * 获取所有查询的摘要信息
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findAllSummaries(int offset, int limit);

    /**
     * 根据数据源获取查询摘要信息
     * @param datasourceName 数据源名称
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findSummariesByDatasource(String datasourceName, int offset, int limit);

    /**
     * 根据表名获取相关查询
     * @param tableName 表名
     * @param datasourceName 数据源名称
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findSummariesByTable(String tableName, String datasourceName, int offset, int limit);

    /**
     * 更新查询统计信息
     * @param queryId 查询ID
     * @param responseTime 响应时间（毫秒）
     * @param cacheHit 是否缓存命中
     */
    void updateStatistics(String queryId, long responseTime, boolean cacheHit);

    /**
     * 更新查询的最后访问时间
     * @param queryId 查询ID
     * @param lastAccessTime 最后访问时间
     */
    void updateLastAccessTime(String queryId, LocalDateTime lastAccessTime);

    /**
     * 删除查询记录
     * @param queryId 查询ID
     * @return 是否删除成功
     */
    boolean deleteById(String queryId);

    /**
     * 删除指定数据源的所有查询记录
     * @param datasourceName 数据源名称
     * @return 删除的记录数
     */
    int deleteByDatasource(String datasourceName);

    /**
     * 获取查询总数
     * @return 查询总数
     */
    long count();

    /**
     * 获取指定数据源的查询总数
     * @param datasourceName 数据源名称
     * @return 查询总数
     */
    long countByDatasource(String datasourceName);

    /**
     * 获取最近访问的查询
     * @param datasourceName 数据源名称（可选）
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findRecentlyAccessed(String datasourceName, int limit);

    /**
     * 获取访问频率最高的查询
     * @param datasourceName 数据源名称（可选）
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findMostFrequent(String datasourceName, int limit);

    /**
     * 获取缓存命中率最高的查询
     * @param datasourceName 数据源名称（可选）
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findHighestCacheHitRate(String datasourceName, int limit);

    /**
     * 批量保存查询信息
     * @param queries 查询对象列表
     * @return 保存的记录数
     */
    int batchSave(List<Query> queries);

    /**
     * 清理过期的查询记录
     * @param expiredBefore 过期时间点
     * @return 清理的记录数
     */
    int cleanupExpiredQueries(LocalDateTime expiredBefore);
}