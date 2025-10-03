package org.openjdbcproxy.cache.service;

import org.openjdbcproxy.cache.entity.CacheDecision;
import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

/**
 * 缓存拦截器服务接口
 * 提供缓存决策、缓存键生成、缓存数据获取等核心功能
 */
public interface CacheInterceptorService {
    
    /**
     * 预处理查询请求，进行缓存决策和缓存查找
     * 
     * @param request 语句请求
     * @param sessionInfo 会话信息
     * @return 缓存处理结果
     */
    CacheProcessResult preProcessQuery(StatementRequest request, SessionInfo sessionInfo);
    
    /**
     * 后处理查询结果，进行缓存存储和指标记录
     * 
     * @param request 语句请求
     * @param executionTimeMs 执行时间（毫秒）
     * @param success 是否执行成功
     */
    void postProcessQuery(StatementRequest request, long executionTimeMs, boolean success);
    
    /**
     * 生成缓存键
     * 
     * @param request 语句请求
     * @param sessionInfo 会话信息
     * @return 缓存键
     */
    String generateCacheKey(StatementRequest request, SessionInfo sessionInfo);
    
    /**
     * 获取缓存数据源连接
     * 
     * @param sessionInfo 会话信息
     * @return 缓存数据源连接
     */
    Connection getCacheDataSourceConnection(SessionInfo sessionInfo);
    
    /**
     * 确保数据源存在
     * 
     * @param connHash 连接哈希
     */
    void ensureDataSource(String connHash);
    
    /**
     * 从SQL中提取表名
     * 
     * @param sql SQL语句
     * @return 表名列表
     */
    List<String> extractTables(String sql);
    
    /**
     * 缓存处理结果
     */
    class CacheProcessResult {
        private final CacheDecision decision;
        private final ResultSet cachedResultSet;
        private final Connection cacheConnection;
        private final long processingStartTime;
        
        public CacheProcessResult(CacheDecision decision, ResultSet cachedResultSet, 
                                Connection cacheConnection, long processingStartTime) {
            this.decision = decision;
            this.cachedResultSet = cachedResultSet;
            this.cacheConnection = cacheConnection;
            this.processingStartTime = processingStartTime;
        }
        
        public CacheDecision getDecision() { return decision; }
        public ResultSet getCachedResultSet() { return cachedResultSet; }
        public Connection getCacheConnection() { return cacheConnection; }
        public long getProcessingStartTime() { return processingStartTime; }
        
        public boolean isHit() { return decision != null && decision.isUseCache() && cachedResultSet != null; }
        public boolean isMiss() { return decision != null && decision.isUseCache() && cachedResultSet == null; }
        public boolean isSkip() { return decision == null || !decision.isUseCache(); }
        
        // 便利方法，用于获取缓存相关信息
        public String getCacheKey() { return decision != null ? decision.getCacheKey() : null; }
        public int getTtl() { return decision != null ? decision.getTtlSeconds() : 0; }
        public String getRuleName() { return decision != null ? decision.getRuleName() : null; }
        public String getSkipReason() { return decision != null ? decision.getSkipReason() : "Unknown"; }
    }
}