package com.redis.smartcache.webapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Redis连接请求模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedisConnectionRequest {
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("port")
    private int port;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password;
    
    @JsonProperty("database")
    private int database;
    
    @JsonProperty("applicationName")
    private String applicationName;
    
    @JsonProperty("ssl")
    private boolean ssl;
    
    @JsonProperty("timeout")
    private long timeout;

    // 构造函数
    public RedisConnectionRequest() {}

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getDatabase() { return database; }
    public void setDatabase(int database) { this.database = database; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public boolean isSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }

    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }
}

