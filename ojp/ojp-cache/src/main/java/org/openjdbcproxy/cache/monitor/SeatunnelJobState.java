package org.openjdbcproxy.cache.monitor;

import lombok.Data;

@Data
public class SeatunnelJobState {
    private String jobId;
    private String jobName;
    private String phase; // SNAPSHOT, INCREMENTAL
    private String status; // RUNNING, FAILED, FINISHED
    private long lag;      // milliseconds
    private long updateTime;

    public boolean isSyncing() {
        return "SNAPSHOT".equalsIgnoreCase(phase) || "RUNNING".equalsIgnoreCase(status) && lag > 5000;
    }
    
    public boolean isReady() {
        // Circuit Breaker: If no heartbeat for > 60s, consider stuck/offline.
        long now = System.currentTimeMillis();
        if (now - updateTime > 60000) {
            return false;
        }
        
        return "INCREMENTAL".equalsIgnoreCase(phase) 
               && "RUNNING".equalsIgnoreCase(status) 
               && lag < 5000;
    }
}
