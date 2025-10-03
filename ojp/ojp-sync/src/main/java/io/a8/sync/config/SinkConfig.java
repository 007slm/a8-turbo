package io.a8.sync.config;

import java.util.Map;

/**
 * 目标数据库配置
 */
public class SinkConfig {
    private String type;
    private String jdbcUrl;
    private String loadUrl;
    private String username;
    private String password;
    private Map<String, String> tableCreateProperties;
    private Long sinkBufferFlushMaxBytes;
    private Integer sinkBufferFlushIntervalMs;
    private Integer sinkBufferFlushMaxRows;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getLoadUrl() {
        return loadUrl;
    }

    public void setLoadUrl(String loadUrl) {
        this.loadUrl = loadUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getTableCreateProperties() {
        return tableCreateProperties;
    }

    public void setTableCreateProperties(Map<String, String> tableCreateProperties) {
        this.tableCreateProperties = tableCreateProperties;
    }

    public Long getSinkBufferFlushMaxBytes() {
        return sinkBufferFlushMaxBytes;
    }

    public void setSinkBufferFlushMaxBytes(Long sinkBufferFlushMaxBytes) {
        this.sinkBufferFlushMaxBytes = sinkBufferFlushMaxBytes;
    }

    public Integer getSinkBufferFlushIntervalMs() {
        return sinkBufferFlushIntervalMs;
    }

    public void setSinkBufferFlushIntervalMs(Integer sinkBufferFlushIntervalMs) {
        this.sinkBufferFlushIntervalMs = sinkBufferFlushIntervalMs;
    }

    public Integer getSinkBufferFlushMaxRows() {
        return sinkBufferFlushMaxRows;
    }

    public void setSinkBufferFlushMaxRows(Integer sinkBufferFlushMaxRows) {
        this.sinkBufferFlushMaxRows = sinkBufferFlushMaxRows;
    }
}