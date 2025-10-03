package org.openjdbcproxy.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 定义缓存相关的配置属性
 * 
 * 主要包含以下配置项：
 * - 基础缓存配置：是否启用、TTL时间范围
 * - 查询统计配置：是否启用、保留天数、批量大小、更新间隔
 * - 缓存规则配置：刷新间隔、最大规则数、默认优先级、是否启用规则缓存
 * - Redis配置：键前缀、分隔符、过期时间、连接和读取超时
 * - 性能配置：最大并发查询数、查询超时、预热线程数、异步处理开关和队列大小
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ojp.cache")
public class CacheConfig {

    private int defaultTtl = 300;
    private int maxTtl = 3600;
    private int minTtl = 10;
    private QueryStats queryStats = new QueryStats();
    private CacheRule cacheRule = new CacheRule();
    private Redis redis = new Redis();
    private Performance performance = new Performance();

    /**
     * 查询统计配置
     * 
     * 包含以下属性：
     * - enabled: 是否启用查询统计
     * - retentionDays: 统计数据保留天数
     * - batchSize: 批量更新大小
     * - updateInterval: 统计更新间隔（秒）
     */
    @Data
    public static class QueryStats {
        private int retentionDays = 30;
        private int batchSize = 100;
        private int updateInterval = 60;
    }

    /**
     * 缓存规则配置
     * 
     * 包含以下属性：
     * - refreshInterval: 规则刷新间隔（秒）
     * - maxRules: 最大规则数量
     * - defaultPriority: 默认优先级
     * - cacheEnabled: 是否启用规则缓存
     */
    @Data
    public static class CacheRule {
        private int refreshInterval = 300;
        private int maxRules = 1000;
        private int defaultPriority = 100;
        private boolean cacheEnabled = true;
    }

    /**
     * Redis配置
     * 
     * 包含以下属性：
     * - keyPrefix: 键前缀
     * - keySeparator: 键分隔符
     * - defaultExpireDays: 默认过期时间（天）
     * - connectionTimeout: 连接超时时间（毫秒）
     * - readTimeout: 读取超时时间（毫秒）
     */
    @Data
    public static class Redis {
        private String keyPrefix = "ojp:rule";
        private String keySeparator = ":";
        private int defaultExpireDays = 7;
        private int connectionTimeout = 5000;
        private int readTimeout = 3000;
    }

    /**
     * 性能配置
     * 
     * 包含以下属性：
     * - maxConcurrentQueries: 最大并发查询数
     * - queryTimeout: 查询超时时间（毫秒）
     * - warmupThreads: 缓存预热线程数
     * - asyncEnabled: 是否启用异步处理
     * - asyncQueueSize: 异步队列大小
     */
    @Data
    public static class Performance {
        private int maxConcurrentQueries = 1000;
        private int queryTimeout = 30000;
        private int warmupThreads = 2;
        private boolean asyncEnabled = true;
        private int asyncQueueSize = 10000;
    }

    /**
     * 获取完整的Redis键
     */
    public String getRedisKey(String... parts) {
        StringBuilder sb = new StringBuilder(redis.keyPrefix);
        for (String part : parts) {
            sb.append(redis.keySeparator).append(part);
        }
        return sb.toString();
    }

    /**
     * 验证TTL是否在有效范围内
     */
    public boolean isValidTtl(int ttl) {
        return ttl >= minTtl && ttl <= maxTtl;
    }

    /**
     * 获取有效的TTL值
     */
    public int getValidTtl(int ttl) {
        if (ttl < minTtl) {
            return minTtl;
        }
        if (ttl > maxTtl) {
            return maxTtl;
        }
        return ttl;
    }
}