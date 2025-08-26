package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis连接状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisConnectionStatus {
    
    /**
     * 是否已连接
     */
    private Boolean connected;
    
    /**
     * PING响应
     */
    private String ping;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
    
    /**
     * 连接详情
     */
    private ConnectionDetails details;
    
    /**
     * 连接详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionDetails {
        /**
         * 主机地址
         */
        private String host;
        
        /**
         * 端口
         */
        private Integer port;
        
        /**
         * 数据库索引
         */
        private Integer database;
        
        /**
         * 连接池大小
         */
        private Integer poolSize;
        
        /**
         * 活跃连接数
         */
        private Integer activeConnections;
        
        /**
         * 空闲连接数
         */
        private Integer idleConnections;
        
        /**
         * 连接超时时间
         */
        private Long connectionTimeout;
        
        /**
         * 读取超时时间
         */
        private Long readTimeout;
    }
}
