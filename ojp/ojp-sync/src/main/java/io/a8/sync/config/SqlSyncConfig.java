package io.a8.sync.config;

import java.util.Map;

/**
 * 配置类：将 SQL 作业脚本转换为 Java API 时使用的配置结构。
 * <p>
 * 该配置描述了全库同步的场景：一个 MySQL 数据库的所有表都被 CDC 捕获，
 * 并以 JSON 形式写入 StarRocks 中的统一表。
 */
public class SqlSyncConfig {

    private SqlSourceConfig source;
    private SqlSinkConfig sink;
    private PipelineConfig pipeline;
    private CheckpointConfig checkpoint;

    public SqlSourceConfig getSource() {
        return source;
    }

    public void setSource(SqlSourceConfig source) {
        this.source = source;
    }

    public SqlSinkConfig getSink() {
        return sink;
    }

    public void setSink(SqlSinkConfig sink) {
        this.sink = sink;
    }

    public PipelineConfig getPipeline() {
        return pipeline;
    }

    public void setPipeline(PipelineConfig pipeline) {
        this.pipeline = pipeline;
    }

    public CheckpointConfig getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(CheckpointConfig checkpoint) {
        this.checkpoint = checkpoint;
    }

    /**
     * MySQL 源配置。
     */
    public static class SqlSourceConfig {
        private String hostname;
        private Integer port;
        private String username;
        private String password;
        private String database;
        private String tablePattern;
        private String serverId;
        private String serverTimeZone;
        private String startupMode;
        private Boolean includeSchemaChanges;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
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

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getTablePattern() {
            return tablePattern;
        }

        public void setTablePattern(String tablePattern) {
            this.tablePattern = tablePattern;
        }

        public String getServerId() {
            return serverId;
        }

        public void setServerId(String serverId) {
            this.serverId = serverId;
        }

        public String getServerTimeZone() {
            return serverTimeZone;
        }

        public void setServerTimeZone(String serverTimeZone) {
            this.serverTimeZone = serverTimeZone;
        }

        public String getStartupMode() {
            return startupMode;
        }

        public void setStartupMode(String startupMode) {
            this.startupMode = startupMode;
        }

        public Boolean getIncludeSchemaChanges() {
            return includeSchemaChanges;
        }

        public void setIncludeSchemaChanges(Boolean includeSchemaChanges) {
            this.includeSchemaChanges = includeSchemaChanges;
        }
    }

    /**
     * StarRocks 目标配置。
     */
    public static class SqlSinkConfig {
        private String jdbcUrl;
        private String loadUrl;
        private String username;
        private String password;
        private String database;
        private String table;
        private Long bufferFlushMaxBytes;
        private Integer bufferFlushIntervalMs;
        private Integer bufferFlushMaxRows;
        private Map<String, String> tableCreateProperties;

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

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public Long getBufferFlushMaxBytes() {
            return bufferFlushMaxBytes;
        }

        public void setBufferFlushMaxBytes(Long bufferFlushMaxBytes) {
            this.bufferFlushMaxBytes = bufferFlushMaxBytes;
        }

        public Integer getBufferFlushIntervalMs() {
            return bufferFlushIntervalMs;
        }

        public void setBufferFlushIntervalMs(Integer bufferFlushIntervalMs) {
            this.bufferFlushIntervalMs = bufferFlushIntervalMs;
        }

        public Integer getBufferFlushMaxRows() {
            return bufferFlushMaxRows;
        }

        public void setBufferFlushMaxRows(Integer bufferFlushMaxRows) {
            this.bufferFlushMaxRows = bufferFlushMaxRows;
        }

        public Map<String, String> getTableCreateProperties() {
            return tableCreateProperties;
        }

        public void setTableCreateProperties(Map<String, String> tableCreateProperties) {
            this.tableCreateProperties = tableCreateProperties;
        }
    }
}
