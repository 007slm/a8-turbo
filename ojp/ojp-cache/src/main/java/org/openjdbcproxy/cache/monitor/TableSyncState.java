package org.openjdbcproxy.cache.monitor;

import lombok.Data;

/**
 * 表级同步状态 - 用于 O(1) 缓存决策
 */
@Data
public class TableSyncState {
    private String tableKey;     // "connHash:table" (规范化后的键)
    private String connHash;
    private String database;
    private String tableName;
    private String jobId;
    private String ruleId;
    private boolean ready;       // Stream 推送的最新状态
    private long updateTime;
    
    /**
     * 判断状态是否过期 (超过60秒无更新视为 stale)
     */
    public boolean isStale() {
        return System.currentTimeMillis() - updateTime > 60_000;
    }
    
    /**
     * 综合判断表是否可用于缓存查询
     */
    public boolean isAvailable() {
        return ready && !isStale();
    }
}
