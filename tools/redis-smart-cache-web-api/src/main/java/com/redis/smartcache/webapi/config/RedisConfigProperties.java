package com.redis.smartcache.webapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis配置属性类
 * 用于绑定application.yml中的Redis配置
 */
@Component
@ConfigurationProperties(prefix = "smartcache.redis")
public class RedisConfigProperties {
    
    private String host = "localhost";
    private int port = 6379;
    private String password = "";
    private String username = "default";
    private int database = 0;
    private long timeout = 10000;
    private boolean ssl = false;
    
    // Constructors
    public RedisConfigProperties() {}
    
    // Getters and Setters
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public int getDatabase() {
        return database;
    }
    
    public void setDatabase(int database) {
        this.database = database;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public boolean isSsl() {
        return ssl;
    }
    
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
    
    /**
     * 构建Redis URI
     */
    public String buildRedisUri() {
        StringBuilder uriBuilder = new StringBuilder();
        
        if (ssl) {
            uriBuilder.append("rediss://");
        } else {
            uriBuilder.append("redis://");
        }
        
        // 添加用户名和密码
        if (username != null && !username.trim().isEmpty() && password != null && !password.trim().isEmpty()) {
            uriBuilder.append(username).append(":").append(password).append("@");
        } else if (password != null && !password.trim().isEmpty()) {
            uriBuilder.append(":").append(password).append("@");
        }
        
        // 添加主机和端口
        uriBuilder.append(host).append(":").append(port);
        
        // 添加数据库
        if (database > 0) {
            uriBuilder.append("/").append(database);
        }
        
        return uriBuilder.toString();
    }
    
    @Override
    public String toString() {
        return "RedisConfigProperties{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", database=" + database +
                ", timeout=" + timeout +
                ", ssl=" + ssl +
                '}';
    }
}