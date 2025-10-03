package org.openjdbcproxy.cache.repository;

import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.dto.QuerySummary;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 缓存查询数据存储接口
 * 负责缓存查询信息的持久化和检索
 */
public interface CacheQueryRepository {

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
     * 根据SQL哈希和连接哈希查找查询
     * @param sqlHash SQL哈希值
     * @param connHash 连接哈希值
     * @return 查询对象
     */
    Optional<Query> findBySqlHash(String sqlHash, String connHash);


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
     * 根据连接哈希删除查询
     * @param connHash 连接哈希值
     * @return 删除的查询数量
     */
    int deleteByDatasource(String connHash);

    /**
     * 获取查询总数
     * @return 查询总数
     */
    long count();

    /**
     * 统计连接哈希的查询数量
     * @param connHash 连接哈希值
     * @return 查询数量
     */
    long countByDatasource(String connHash);

    List<QuerySummary> findSummariesByConnHash(String connHash, int offset, int limit);

    List<QuerySummary> findSummariesByTable(String tableName, String connHash, int offset, int limit);

    /**
     * 获取最近访问的查询
     * @param connHash 连接哈希值（可选）
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findRecentlyAccessed(String connHash, int limit);

    /**
     * 获取最频繁的查询
     * @param connHash 连接哈希值（可选）
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findMostFrequent(String connHash, int limit);

    /**
     * 获取缓存命中率最高的查询
     * @param connHash 连接哈希值（可选）
     * @param limit 限制数量
     * @return 查询摘要列表
     */
    List<QuerySummary> findHighestCacheHitRate(String connHash, int limit);

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