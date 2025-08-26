package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis健康状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisHealthInfo {
    
    /**
     * 连接状态 (UP/DOWN/ERROR)
     */
    private String status;
    
    /**
     * 是否已连接
     */
    private Boolean connected;
    
    /**
     * Redis主机地址
     */
    private String host;
    
    /**
     * Redis端口
     */
    private Integer port;
    
    /**
     * 连接是否打开
     */
    private Boolean connectionOpen;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 状态消息
     */
    private String message;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
    
    /**
     * Redis详细信息
     */
    private RedisDetailsInfo details;
    
    /**
     * Redis详细信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisDetailsInfo {
        /**
         * Redis版本
         */
        private String version;
        
        /**
         * 已连接客户端数
         */
        private Integer connectedClients;
        
        /**
         * 已使用内存
         */
        private Long usedMemory;
        
        /**
         * 最大内存
         */
        private Long maxMemory;
        
        /**
         * 内存使用率
         */
        private Double memoryUsagePercent;
        
        /**
         * 数据库数量
         */
        private Integer databases;
        
        /**
         * 键数量
         */
        private Long keyspace;
        
        /**
         * 命中次数
         */
        private Long hits;
        
        /**
         * 未命中次数
         */
        private Long misses;
        
        /**
         * 命中率
         */
        private Double hitRate;
    }
}
