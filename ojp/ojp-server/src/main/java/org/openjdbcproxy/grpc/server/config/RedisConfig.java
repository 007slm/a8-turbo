package org.openjdbcproxy.grpc.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ojp.redis")
public class RedisConfig {
    
    /**
     * 是否启用Redis
     */
    private Boolean enabled = true;
    
    /**
     * Redis主机地址
     */
    private String host = "localhost";
    
    /**
     * Redis端口
     */
    private Integer port = 6379;
    
    /**
     * Redis密码
     */
    private String password = "";
    
    /**
     * 数据库索引
     */
    private Integer database = 0;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer timeout = 10000;
    
    /**
     * 最大活跃连接数
     */
    private Integer maxActive = 20;
    
    /**
     * 最大空闲连接数
     */
    private Integer maxIdle = 10;
    
    /**
     * 最小空闲连接数
     */
    private Integer minIdle = 5;
    
    /**
     * 最大等待时间（毫秒）
     */
    private Integer maxWait = 2000;
    
    /**
     * 是否启用SSL
     */
    private Boolean ssl = false;
}
