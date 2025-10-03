package org.openjdbcproxy.cache.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RedisHash("ojp:stats:slow:data")
@Data
public class SlowQuery {
    @Id
    private String id;
    
    private String sql;
    private String parameters;
    private long executionTime;
    private boolean inTransaction;
    private boolean hasError;
    private long timestamp;
    private String clientUUID;
    private String connHash;
    private String methodName;
    private String normalizedSql;
    private String queryType;
    private String tableNames;


//    private LocalDateTime createdAt = LocalDateTime.now();
//    private LocalDateTime lastAccessTime = LocalDateTime.now();
//    private Long accessCount;
//    private Double avgResponseTime;
//    private Long cacheHitCount;
//    private Double cacheHitRate;
//    private Map<String, Object> attributes;
//    @Reference
//    private List<CacheRule> matchedRules = new ArrayList<>();

    // 更新统计信息
//    public void updateStatistics(long responseTime, boolean cacheHit) {
//        this.lastAccessTime = LocalDateTime.now();
//        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
//
//        if (cacheHit) {
//            this.cacheHitCount = (this.cacheHitCount == null ? 0 : this.cacheHitCount) + 1;
//        }
//
//        // 更新平均响应时间
//        if (this.avgResponseTime == null) {
//            this.avgResponseTime = (double) responseTime;
//        } else {
//            this.avgResponseTime = (this.avgResponseTime * (this.accessCount - 1) + responseTime) / this.accessCount;
//        }
//
//        // 更新缓存命中率
//        if (this.cacheHitCount != null && this.accessCount != null && this.accessCount > 0) {
//            this.cacheHitRate = (double) this.cacheHitCount / this.accessCount;
//        }
//    }

}