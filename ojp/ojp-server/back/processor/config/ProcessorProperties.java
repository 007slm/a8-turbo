package org.openjdbcproxy.grpc.server.processor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 处理器配置属性类
 * 提供类型安全的配置管理
 * 
 * 注意：不提供处理器启用/禁用配置，处理器的启用由 Spring Bean 注册决定
 * 
 * 使用示例：
 * - application.yml 配置
 * - 环境变量配置
 * - 系统属性配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ojp")
public class ProcessorProperties {
    
    /**
     * 审计日志配置
     */
    private final Audit audit = new Audit();
    
    /**
     * 安全验证配置
     */
    private final Security security = new Security();
    
    @Data
    public static class Audit {
        
        /**
         * 是否记录连接操作
         */
        private boolean logConnections = true;
        
        /**
         * 是否记录查询操作
         */
        private boolean logQueries = true;
        
        /**
         * 是否记录事务操作
         */
        private boolean logTransactions = true;
        
        /**
         * 是否记录 LOB 操作
         */
        private boolean logLobOperations = false;
        
        /**
         * 是否记录资源调用
         */
        private boolean logResourceCalls = false;
        
        /**
         * 是否记录 SQL 参数
         */
        private boolean logSqlParameters = false;
        
        /**
         * SQL 日志最大长度
         */
        private int maxSqlLength = 1000;
    }
    
    @Data
    public static class Security {
        
        /**
         * 是否启用 SQL 注入检测
         */
        private boolean sqlInjectionDetection = true;
        
        /**
         * 是否阻止危险操作
         */
        private boolean blockDangerousOperations = true;
        
        /**
         * 是否启用频率限制
         */
        private boolean rateLimit = false;
        
        /**
         * 最大请求大小（字节）
         */
        private long maxRequestSize = 10 * 1024 * 1024; // 10MB
        
        /**
         * 最大 LOB 大小（字节）
         */
        private long maxLobSize = 100 * 1024 * 1024; // 100MB
        
        /**
         * 最大并发事务数
         */
        private int maxConcurrentTransactions = 100;
    }
}
