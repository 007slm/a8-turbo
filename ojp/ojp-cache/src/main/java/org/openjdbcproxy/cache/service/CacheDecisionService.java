package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.monitor.TableSyncStateManager;
import org.openjdbcproxy.cache.monitor.metrics.CacheMetrics;
import org.openjdbcproxy.cache.util.JSqlParserUtil;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 缓存决策服务 - 基于表级倒排索引的 O(1) 决策
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheDecisionService {

    private final TableSyncStateManager tableSyncStateManager;
    private final CacheMetrics cacheMetrics;

    /**
     * 判断给定 SQL 是否应该路由到 StarRocks 缓存
     * 
     * @param connHash 连接标识
     * @param sql SQL 语句
     * @return true 如果所有涉及的表都已同步就绪，可以走 StarRocks
     */
    public boolean makeDecision(String connHash, String sql) {
        long startTime = System.currentTimeMillis();
        
        // 解析 SQL 中涉及的表名
        Set<String> involvedTables = JSqlParserUtil.extractTableNames(sql);
        
        if (involvedTables.isEmpty()) {
            log.debug("No tables extracted from SQL, fallback to source");
            recordDecision(startTime, false);
            return false;
        }
        
        // 检查每个表的同步状态
        for (String table : involvedTables) {
            if (!tableSyncStateManager.isTableReady(connHash, table)) {
                long decisionTime = System.currentTimeMillis() - startTime;
                log.info("缓存决策: 表 {} 未就绪, 回源查询. decisionTime={}ms", table, decisionTime);
                recordDecision(startTime, false);
                return false;
            }
        }
        
        long decisionTime = System.currentTimeMillis() - startTime;
        log.info("缓存决策: 所有表就绪 ({}), 走 StarRocks. decisionTime={}ms", involvedTables, decisionTime);
        recordDecision(startTime, true);
        return true;
    }
    
    private void recordDecision(long startTime, boolean hit) {
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.recordDecisionLatency(duration);
        if (hit) {
            cacheMetrics.recordCacheHit();
        } else {
            cacheMetrics.recordCacheMiss();
        }
    }
    
    /**
     * 记录缓存查询延迟 (命中 StarRocks 时调用)
     */
    public void recordCachedQueryLatency(long durationMs) {
        cacheMetrics.recordCachedQueryLatency(durationMs);
    }
    
    /**
     * 记录原始查询延迟 (回源时调用)
     */
    public void recordOriginalQueryLatency(long durationMs) {
        cacheMetrics.recordOriginalQueryLatency(durationMs);
    }
}