package org.openjdbcproxy.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 查询对象
 * 封装SQL查询的相关信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Query {

    /**
     * 查询ID（通过SQL生成的唯一标识）
     */
    private String queryId;

    /**
     * 原始SQL语句
     */
    private String sql;

    /**
     * 规范化的SQL语句（去除参数）
     */
    private String normalizedSql;

    /**
     * 查询涉及的表名列表
     */
    private List<String> tables;

    /**
     * SQL参数
     */
    private List<Object> parameters;

    /**
     * 参数哈希值（用于缓存键生成）
     */
    private String parameterHash;

    /**
     * 查询类型（SELECT, INSERT, UPDATE, DELETE等）
     */
    private QueryType queryType;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

    /**
     * 访问次数
     */
    private Long accessCount;

    /**
     * 平均响应时间（毫秒）
     */
    private Double avgResponseTime;

    /**
     * 缓存命中次数
     */
    private Long cacheHitCount;

    /**
     * 缓存命中率
     */
    private Double cacheHitRate;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;

    /**
     * 查询类型枚举
     */
    public enum QueryType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        CREATE,
        DROP,
        ALTER,
        UNKNOWN
    }

    /**
     * 是否为查询操作
     */
    public boolean isSelectQuery() {
        return QueryType.SELECT.equals(this.queryType);
    }

    /**
     * 是否为写操作
     */
    public boolean isWriteOperation() {
        return QueryType.INSERT.equals(this.queryType) ||
               QueryType.UPDATE.equals(this.queryType) ||
               QueryType.DELETE.equals(this.queryType);
    }

    /**
     * 获取缓存键
     */
    public String getCacheKey() {
        if (parameterHash != null && !parameterHash.isEmpty()) {
            return String.format("%s:cache:%s:%s", datasourceName, queryId, parameterHash);
        } else {
            return String.format("%s:cache:%s", datasourceName, queryId);
        }
    }

    /**
     * 更新统计信息
     */
    public void updateStatistics(long responseTime, boolean cacheHit) {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        
        if (cacheHit) {
            this.cacheHitCount = (this.cacheHitCount == null ? 0 : this.cacheHitCount) + 1;
        }
        
        // 更新平均响应时间
        if (this.avgResponseTime == null) {
            this.avgResponseTime = (double) responseTime;
        } else {
            this.avgResponseTime = (this.avgResponseTime * (this.accessCount - 1) + responseTime) / this.accessCount;
        }
        
        // 更新缓存命中率
        if (this.cacheHitCount != null && this.accessCount != null && this.accessCount > 0) {
            this.cacheHitRate = (double) this.cacheHitCount / this.accessCount;
        }
    }

    /**
     * 更新统计信息
     */
    public void updateStats(long responseTime, boolean fromCache) {
        this.lastAccessTime = LocalDateTime.now();
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        
        if (fromCache) {
            this.cacheHitCount = (this.cacheHitCount == null ? 0 : this.cacheHitCount) + 1;
        }
        
        // 更新平均响应时间
        if (this.avgResponseTime == null) {
            this.avgResponseTime = (double) responseTime;
        } else {
            this.avgResponseTime = (this.avgResponseTime * (this.accessCount - 1) + responseTime) / this.accessCount;
        }
        
        // 更新缓存命中率
        if (this.accessCount > 0) {
            this.cacheHitRate = (double) (this.cacheHitCount == null ? 0 : this.cacheHitCount) / this.accessCount;
        }
    }
}