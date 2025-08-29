package org.openjdbcproxy.grpc.server.processor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理器配置类
 * 支持通过配置文件配置处理器的行为
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ojp.processor")
public class ProcessorConfiguration {

    /**
     * 全局处理器启用开关
     */
    private boolean enabled = true;

    /**
     * 性能监控处理器配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 审计日志处理器配置
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * 安全处理器配置
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 缓存处理器配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 自定义处理器配置
     */
    private Map<String, Object> custom = new HashMap<>();

    @Data
    public static class PerformanceConfig {
        private boolean enabled = true;
        private int slowQueryThresholdMs = 1000;
        private boolean enableMetrics = true;
        private boolean logSlowQueries = true;
    }

    @Data
    public static class AuditConfig {
        private boolean enabled = true;
        private boolean logConnections = true;
        private boolean logQueries = true;
        private boolean logTransactions = true;
        private boolean includeParameters = false;
        private int maxParameterLength = 1000;
    }

    @Data
    public static class SecurityConfig {
        private boolean enabled = true;
        private boolean validateSql = true;
        private boolean blockDangerousOperations = false;
        private int maxQueryLength = 10000;
        private String[] blockedKeywords = {"DROP", "DELETE", "TRUNCATE"};
    }

    @Data
    public static class CacheConfig {
        private boolean enabled = false;
        private int maxCacheSize = 1000;
        private int cacheExpirationMinutes = 60;
        private boolean cacheQueries = true;
        private boolean cacheConnections = false;
    }
}
