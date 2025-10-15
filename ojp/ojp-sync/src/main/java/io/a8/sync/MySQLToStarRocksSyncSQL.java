package io.a8.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.starrocks.connector.flink.StarRocksSink;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import io.a8.sync.config.CheckpointConfig;
import io.a8.sync.config.PipelineConfig;
import io.a8.sync.config.SqlSyncConfig;
import io.a8.sync.config.SqlSyncConfig.SqlSinkConfig;
import io.a8.sync.config.SqlSyncConfig.SqlSourceConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@code docker/flink/jobs/mysql-to-starrocks.sql} 中 Table API 作业的 Java 版本实现。
 *
 * <p>功能特性：
 * <ul>
 *     <li>捕获指定 MySQL 数据库下的所有表（可通过正则控制范围）</li>
 *     <li>将每条 CDC 事件封装为 JSON，写入 StarRocks 统一表</li>
 *     <li>自动将 LONGBLOB/BLOB 等二进制字段转为 Base64，避免 BYTES 类型被拒绝</li>
 * </ul>
 */
public class MySQLToStarRocksSyncSQL {

    private static final Logger LOG = LoggerFactory.getLogger(MySQLToStarRocksSyncSQL.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 0) {
            LOG.warn("检测到传入参数，将忽略外部配置文件，使用代码内置配置。");
        }

        SqlSyncConfig config = buildDefaultConfig();
        validateConfig(config);

        StreamExecutionEnvironment env = buildExecutionEnvironment(config);
        MySqlSource<String> mysqlSource = buildMySqlSource(config);

        DataStreamSource<String> cdcStream = env.fromSource(
                mysqlSource,
                WatermarkStrategy.noWatermarks(),
                "mysql-cdc-source");

        ZoneId eventZone = resolveZoneId(config.getSource().getServerTimeZone());

        cdcStream
                .flatMap((FlatMapFunction<String, String>) (value, out) -> {
                    try {
                        JsonNode root = MAPPER.readTree(value);

                        JsonNode sourceNode = root.path("source");
                        if (sourceNode.isMissingNode()) {
                            LOG.debug("忽略缺少 source 节点的事件 {}", abbreviate(value));
                            return;
                        }
                        String databaseName = textValue(sourceNode.get("db"));
                        String tableName = textValue(sourceNode.get("table"));
                        if (StringUtils.isNullOrWhitespaceOnly(databaseName)
                                || StringUtils.isNullOrWhitespaceOnly(tableName)) {
                            LOG.debug("忽略无效的库/表信息 {}", abbreviate(value));
                            return;
                        }

                        String operation = translateOperation(root.path("op").asText(""));
                        JsonNode dataNode = selectDataNode(root);
                        if (dataNode == null || dataNode.isNull()) {
                            LOG.debug("忽略空数据事件 {}", abbreviate(value));
                            return;
                        }

                        sanitizeBinaryNodes(dataNode);
                        String payload = MAPPER.writeValueAsString(dataNode);

                        long tsMs = root.path("ts_ms").asLong(System.currentTimeMillis());
                        String syncTime = TS_FORMATTER.format(
                                Instant.ofEpochMilli(tsMs).atZone(eventZone));

                        ObjectNode outNode = MAPPER.createObjectNode();
                        outNode.put("database_name", databaseName);
                        outNode.put("table_name", tableName);
                        outNode.put("operation", operation);
                        outNode.put("data", payload);
                        outNode.put("sync_time", syncTime);

                        out.collect(MAPPER.writeValueAsString(outNode));
                    } catch (Exception ex) {
                        LOG.error("解析 CDC 事件失败，内容 {}", abbreviate(value), ex);
                    }
                })
                .returns(org.apache.flink.api.common.typeinfo.Types.STRING)
                .name("cdc-event-formatter")
                .addSink(buildStarRocksSink(config))
                .name("starrocks-json-sink")
                .setParallelism(config.getPipeline().getParallelism());

        env.execute(config.getPipeline().getName());
    }

    private static SqlSyncConfig buildDefaultConfig() {
        SqlSyncConfig config = new SqlSyncConfig();

        SqlSourceConfig source = new SqlSourceConfig();
        source.setHostname("mysql");
        source.setPort(3306);
        source.setUsername("root");
        source.setPassword("a8");
        source.setDatabase("shopdb");
        source.setTablePattern(null);
        source.setServerId("5401");
        source.setServerTimeZone("UTC");
        source.setStartupMode("initial");
        source.setIncludeSchemaChanges(false);
        config.setSource(source);

        SqlSinkConfig sink = new SqlSinkConfig();
        sink.setJdbcUrl("jdbc:mysql://starrocks:9030");
        sink.setLoadUrl("starrocks:8030");
        sink.setUsername("root");
        sink.setPassword("");
        sink.setDatabase("shopdb");
        sink.setTable("sync_all_tables");
        sink.setBufferFlushIntervalMs(5_000);
        sink.setBufferFlushMaxRows(64000);
        config.setSink(sink);

        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("mysql-to-starrocks-sql-job");
        pipeline.setParallelism(1);
        config.setPipeline(pipeline);

        CheckpointConfig checkpoint = new CheckpointConfig();
        checkpoint.setInterval(60_000L);
        checkpoint.setTimeout(600_000L);
        checkpoint.setMinPause(10_000L);
        checkpoint.setTolerableFailureNum(3);
        config.setCheckpoint(checkpoint);

        return config;
    }

    private static void validateConfig(SqlSyncConfig config) {
        Objects.requireNonNull(config, "初始化配置失败");

        SqlSourceConfig source = Objects.requireNonNull(config.getSource(), "source 配置缺失");
        Objects.requireNonNull(source.getHostname(), "source.hostname 未配置");
        Objects.requireNonNull(source.getPort(), "source.port 未配置");
        Objects.requireNonNull(source.getUsername(), "source.username 未配置");
        Objects.requireNonNull(source.getPassword(), "source.password 未配置");
        Objects.requireNonNull(source.getDatabase(), "source.database 未配置");
        Objects.requireNonNull(source.getServerId(), "source.serverId 未配置");
        Objects.requireNonNull(source.getServerTimeZone(), "source.serverTimeZone 未配置");

        SqlSinkConfig sink = Objects.requireNonNull(config.getSink(), "sink 配置缺失");
        Objects.requireNonNull(sink.getJdbcUrl(), "sink.jdbcUrl 未配置");
        Objects.requireNonNull(sink.getLoadUrl(), "sink.loadUrl 未配置");
        Objects.requireNonNull(sink.getUsername(), "sink.username 未配置");
        Objects.requireNonNull(sink.getDatabase(), "sink.database 未配置");
        Objects.requireNonNull(sink.getTable(), "sink.table 未配置");

        PipelineConfig pipeline = Objects.requireNonNull(config.getPipeline(), "pipeline 配置缺失");
        Objects.requireNonNull(pipeline.getName(), "pipeline.name 未配置");
        Objects.requireNonNull(pipeline.getParallelism(), "pipeline.parallelism 未配置");

        CheckpointConfig checkpoint =
                Objects.requireNonNull(config.getCheckpoint(), "checkpoint 配置缺失");
        Objects.requireNonNull(checkpoint.getInterval(), "checkpoint.interval 未配置");
        Objects.requireNonNull(checkpoint.getTimeout(), "checkpoint.timeout 未配置");
        Objects.requireNonNull(checkpoint.getMinPause(), "checkpoint.minPause 未配置");
    }

    private static StreamExecutionEnvironment buildExecutionEnvironment(SqlSyncConfig config) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        PipelineConfig pipeline = config.getPipeline();
        env.setParallelism(pipeline.getParallelism());

        CheckpointConfig cp = config.getCheckpoint();
        env.enableCheckpointing(cp.getInterval(), CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setCheckpointTimeout(cp.getTimeout());
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(cp.getMinPause());
        if (cp.getTolerableFailureNum() != null) {
            env.getCheckpointConfig()
                    .setTolerableCheckpointFailureNumber(cp.getTolerableFailureNum());
        }
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.setStateBackend(new HashMapStateBackend());
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.seconds(10)));

        return env;
    }

    private static MySqlSource<String> buildMySqlSource(SqlSyncConfig config) {
        SqlSourceConfig source = config.getSource();
        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname(source.getHostname())
                .port(source.getPort())
                .username(source.getUsername())
                .password(source.getPassword())
                .databaseList(source.getDatabase())
                .serverId(source.getServerId())
                .serverTimeZone(source.getServerTimeZone())
                .deserializer(new JsonDebeziumDeserializationSchema());

        if (!StringUtils.isNullOrWhitespaceOnly(source.getTablePattern())) {
            String pattern = source.getTablePattern().trim();
            if (pattern.contains(",")) {
                String[] parts = pattern.split(",");
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                builder.tableList(parts);
            } else {
                builder.tableList(pattern);
            }
        } else {
            // Flink CDC 2.4.x requires non-null tableList; match all tables in the database
            builder.tableList(source.getDatabase() + ".*");
        }

        boolean includeSchema = Boolean.TRUE.equals(source.getIncludeSchemaChanges());
        builder.includeSchemaChanges(includeSchema);

        String mode = source.getStartupMode();
        if ("latest-offset".equalsIgnoreCase(mode) || "latest".equalsIgnoreCase(mode)) {
            builder.startupOptions(StartupOptions.latest());
        } else {
            builder.startupOptions(StartupOptions.initial());
        }
        return builder.build();
    }

    private static SinkFunction<String> buildStarRocksSink(SqlSyncConfig config) {
        SqlSinkConfig sink = config.getSink();

        StarRocksSinkOptions.Builder builder = StarRocksSinkOptions.builder()
                .withProperty("jdbc-url", sink.getJdbcUrl())
                .withProperty("load-url", sink.getLoadUrl())
                .withProperty("username", sink.getUsername())
                .withProperty("password",
                        StringUtils.isNullOrWhitespaceOnly(sink.getPassword()) ? "" : sink.getPassword())
                .withProperty("database-name", sink.getDatabase())
                .withProperty("table-name", sink.getTable())
                // Force V1 stream load via FE to avoid BE 127.0.0.1 advertising issues
                .withProperty("sink.version", "V1")
                .withProperty("sink.properties.format", "json")
                .withProperty("sink.properties.strip_outer_array", "true")
                .withProperty("sink.properties.enable_profile", "false");

        if (sink.getBufferFlushMaxBytes() != null) {
            builder.withProperty("sink.buffer-flush.max-bytes",
                    String.valueOf(sink.getBufferFlushMaxBytes()));
        }
        if (sink.getBufferFlushIntervalMs() != null) {
            builder.withProperty("sink.buffer-flush.interval-ms",
                    String.valueOf(sink.getBufferFlushIntervalMs()));
        }
        if (sink.getBufferFlushMaxRows() != null) {
            builder.withProperty("sink.buffer-flush.max-rows",
                    String.valueOf(sink.getBufferFlushMaxRows()));
        }

        Map<String, String> props = sink.getTableCreateProperties();
        if (props != null && !props.isEmpty()) {
            props.forEach((k, v) ->
                    builder.withProperty("table-create.properties." + k, v));
        }

        return StarRocksSink.sink(builder.build());
    }

    private static JsonNode selectDataNode(JsonNode root) {
        JsonNode payload = root.get("after");
        if (payload == null || payload.isNull()) {
            payload = root.get("before");
        }
        return payload;
    }

    private static String translateOperation(String op) {
        if (op == null) {
            return "UNKNOWN";
        }
        switch (op) {
            case "c":
            case "r":
                return "INSERT";
            case "u":
                return "UPDATE";
            case "d":
                return "DELETE";
            default:
                return "UNKNOWN";
        }
    }

    private static String textValue(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 512) {
            return value;
        }
        return new String(bytes, 0, 512, StandardCharsets.UTF_8) + "...";
    }

    private static ZoneId resolveZoneId(String zone) {
        if (StringUtils.isNullOrWhitespaceOnly(zone)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(zone);
        } catch (Exception ex) {
            LOG.warn("无法解析时区 {}，使用系统默认值 {}", zone, ZoneId.systemDefault());
            return ZoneId.systemDefault();
        }
    }

    /**
     * 将 JSON 节点中的二进制内容转换为 Base64 文本，避免 StarRocks Sink 输出 BYTES 类型。
     */
    private static void sanitizeBinaryNodes(JsonNode node) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<Map.Entry<String, JsonNode>> replacements = new ArrayList<>();

            Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String fieldName = entry.getKey();
                JsonNode child = entry.getValue();

                if (child == null || child.isNull()) {
                    continue;
                }

                if (child.isBinary()) {
                    try {
                        String encoded =
                                Base64.getEncoder().encodeToString(child.binaryValue());
                        replacements.add(new AbstractMap.SimpleEntry<>(fieldName,
                                TextNode.valueOf(encoded)));
                    } catch (Exception e) {
                        LOG.warn("转换二进制字段 {} 失败，保留原值", fieldName, e);
                    }
                } else if (looksLikeBinaryWrapper(child)) {
                    replacements.add(new AbstractMap.SimpleEntry<>(fieldName,
                            TextNode.valueOf(extractBinaryString(child))));
                } else {
                    sanitizeBinaryNodes(child);
                }
            }

            for (Map.Entry<String, JsonNode> replacement : replacements) {
                objectNode.set(replacement.getKey(), replacement.getValue());
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode element : arrayNode) {
                sanitizeBinaryNodes(element);
            }
        }
    }

    private static boolean looksLikeBinaryWrapper(JsonNode node) {
        if (!node.isObject()) {
            return false;
        }
        ObjectNode obj = (ObjectNode) node;
        return obj.has("base64")
                || obj.has("$binary")
                || obj.has("bytes")
                || (obj.has("payload") && obj.get("payload").isBinary());
    }

    private static String extractBinaryString(JsonNode node) {
        if (!node.isObject()) {
            return node.asText("");
        }
        ObjectNode obj = (ObjectNode) node;
        if (obj.has("base64")) {
            return obj.get("base64").asText("");
        }
        if (obj.has("$binary")) {
            return obj.get("$binary").asText("");
        }
        if (obj.has("bytes") && obj.get("bytes").isArray()) {
            ArrayNode array = (ArrayNode) obj.get("bytes");
            byte[] data = new byte[array.size()];
            for (int i = 0; i < array.size(); i++) {
                data[i] = (byte) (array.get(i).asInt() & 0xFF);
            }
            return Base64.getEncoder().encodeToString(data);
        }
        if (obj.has("payload")) {
            JsonNode payload = obj.get("payload");
            if (payload.isBinary()) {
                try {
                    return Base64.getEncoder().encodeToString(payload.binaryValue());
                } catch (Exception e) {
                    LOG.warn("payload 转换失败，返回 JSON 文本", e);
                    return payload.toString();
                }
            }
            return payload.asText("");
        }
        return obj.toString();
    }
}
