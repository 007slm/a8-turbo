package io.a8.sync;

import io.a8.sync.config.SyncConfig;
import io.a8.sync.config.SourceConfig;
import io.a8.sync.config.SinkConfig;
import io.a8.sync.config.PipelineConfig;
import io.a8.sync.config.CheckpointConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrocks.connector.flink.StarRocksSink;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 生产级MySQL到StarRocks数据同步程序
 * 特性: 多表同步、断点续传、自动故障恢复、完善的监控日志
 */
public class MySQLToStarRocksSync {
    private static final Logger LOG = LoggerFactory.getLogger(MySQLToStarRocksSync.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);

    public static void main(String[] args) {
        LOG.info("=== 启动MySQL到StarRocks数据同步程序 ===");

        try {
            // 加载配置
            SyncConfig config = loadConfig(args);
            validateConfig(config);

            // 初始化Flink环境
            StreamExecutionEnvironment env = initializeFlinkEnv(config);

            // 解析表信息
            Map<String, OutputTag<String>> tableTags = initializeTableTags(config);

            // 创建数据源
            DataStream<String> cdcStream = createMySqlSource(env, config);

            // 处理并路由数据
            SingleOutputStreamOperator<String> mainStream = processAndRouteData(cdcStream, tableTags);

            // 创建Sink并写入数据
            createStarRocksSinks(mainStream, tableTags, config);

            // 注册关闭钩子
            registerShutdownHook();

            // 执行作业
            env.execute(config.getPipeline().getName());

        } catch (Exception e) {
            LOG.error("同步程序启动失败", e);
            System.exit(1);
        }
    }

    /**
     * 加载配置文件
     */
    private static SyncConfig loadConfig(String[] args) throws Exception {
        String configPath = "sync-config.yaml";
        if (args.length > 0) {
            configPath = args[0];
        }
        LOG.info("加载配置文件: {}", configPath);

        try (InputStream inputStream = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml(new Constructor(SyncConfig.class));
            return yaml.load(inputStream);
        } catch (Exception e) {
            LOG.error("配置文件加载失败: {}", configPath, e);
            throw e;
        }
    }

    /**
     * 验证配置的完整性
     */
    private static void validateConfig(SyncConfig config) {
        LOG.info("验证配置完整性...");

        SourceConfig source = config.getSource();
        SinkConfig sink = config.getSink();
        PipelineConfig pipeline = config.getPipeline();
        CheckpointConfig checkpoint = config.getCheckpoint();

        // 验证源配置
        Objects.requireNonNull(source.getHostname(), "源数据库主机名未配置");
        Objects.requireNonNull(source.getPort(), "源数据库端口未配置");
        Objects.requireNonNull(source.getUsername(), "源数据库用户名未配置");
        Objects.requireNonNull(source.getPassword(), "源数据库密码未配置");
        Objects.requireNonNull(source.getTables(), "源数据库表列表未配置");
        Objects.requireNonNull(source.getServerId(), "源数据库ServerId未配置");
        Objects.requireNonNull(source.getServerTimeZone(), "源数据库时区未配置");

        // 验证目标配置
        Objects.requireNonNull(sink.getJdbcUrl(), "目标数据库JDBC URL未配置");
        Objects.requireNonNull(sink.getLoadUrl(), "目标数据库Load URL未配置");
        Objects.requireNonNull(sink.getUsername(), "目标数据库用户名未配置");
        Objects.requireNonNull(sink.getPassword(), "目标数据库密码未配置");

        // 验证管道配置
        Objects.requireNonNull(pipeline.getName(), "管道名称未配置");
        Objects.requireNonNull(pipeline.getParallelism(), "并行度未配置");

        // 验证检查点配置
        Objects.requireNonNull(checkpoint.getInterval(), "检查点间隔未配置");

        LOG.info("配置验证通过");
    }

    /**
     * 初始化Flink执行环境
     */
    private static StreamExecutionEnvironment initializeFlinkEnv(SyncConfig config) {
        LOG.info("初始化Flink执行环境...");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 配置检查点
        CheckpointConfig checkpoint = config.getCheckpoint();
        env.enableCheckpointing(checkpoint.getInterval());
        env.getCheckpointConfig().setCheckpointTimeout(checkpoint.getTimeout() != null ?
                checkpoint.getTimeout() : 60000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(
                checkpoint.getMinPause() != null ? checkpoint.getMinPause() : 30000);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(
                checkpoint.getTolerableFailureNum() != null ? checkpoint.getTolerableFailureNum() : 3);

        // 配置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                3, // 最多重启3次
                Time.of(5, TimeUnit.SECONDS) // 每次重启间隔5秒
        ));

        // 设置并行度
        env.setParallelism(config.getPipeline().getParallelism());

        LOG.info("Flink执行环境初始化完成");
        return env;
    }

    /**
     * 初始化表标签映射
     */
    private static Map<String, OutputTag<String>> initializeTableTags(SyncConfig config) {
        LOG.info("初始化表标签映射...");

        Map<String, OutputTag<String>> tableTags = new HashMap<>();
        String[] tables = config.getSource().getTables().split(",");

        for (String table : tables) {
            String trimmedTable = table.trim();
            if (!trimmedTable.isEmpty()) {
                tableTags.put(trimmedTable, new OutputTag<String>(trimmedTable) {});
                LOG.info("添加表标签: {}", trimmedTable);
            }
        }

        if (tableTags.isEmpty()) {
            throw new IllegalArgumentException("未配置任何需要同步的表");
        }

        return tableTags;
    }

    /**
     * 创建MySQL CDC数据源
     */
    private static DataStream<String> createMySqlSource(StreamExecutionEnvironment env, SyncConfig config) {
        LOG.info("创建MySQL CDC数据源...");

        SourceConfig source = config.getSource();
        String[] tables = source.getTables().split(",");
        Set<String> databases = new HashSet<>();

        // 提取数据库名
        for (String table : tables) {
            String[] parts = table.trim().split("\\.");
            if (parts.length >= 1) {
                databases.add(parts[0]);
            }
        }

        MySqlSource<String> sourceFunction = MySqlSource.<String>builder()
                .hostname(source.getHostname())
                .port(source.getPort())
                .username(source.getUsername())
                .password(source.getPassword())
                .databaseList(databases.toArray(new String[0]))
                .tableList(tables)
                .serverId(source.getServerId())
                .serverTimeZone(source.getServerTimeZone())
                .deserializer(new JsonDebeziumDeserializationSchema())
                .includeSchemaChanges(false) // 不包含schema变更
                .build();

        DataStream<String> cdcStream = env.fromSource(
                sourceFunction,
                WatermarkStrategy.noWatermarks(),
                "MySQL CDC Source"
        );

        LOG.info("MySQL CDC数据源创建完成");
        return cdcStream;
    }

    /**
     * 处理CDC数据并按表路由
     */
    private static SingleOutputStreamOperator<String> processAndRouteData(
            DataStream<String> cdcStream,
            Map<String, OutputTag<String>> tableTags) {

        LOG.info("配置数据处理和路由逻辑...");

        return cdcStream.process(new ProcessFunction<String, String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void processElement(String value, Context ctx, Collector<String> out) throws Exception {
                try {
                    JsonNode jsonNode = objectMapper.readTree(value);
                    JsonNode sourceNode = jsonNode.get("source");

                    if (sourceNode != null && sourceNode.has("db") && sourceNode.has("table")) {
                        String db = sourceNode.get("db").asText();
                        String table = sourceNode.get("table").asText();
                        String fullTableName = db + "." + table;

                        // 记录数据量指标（可替换为Metrics）
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("接收到数据 - 表: {}", fullTableName);
                        }

                        // 路由到对应的侧输出流
                        OutputTag<String> outputTag = tableTags.get(fullTableName);
                        if (outputTag != null) {
                            ctx.output(outputTag, value);
                        } else {
                            LOG.warn("收到未配置的表数据: {}", fullTableName);
                        }
                    } else {
                        LOG.warn("无法解析CDC事件的源信息: {}", value.substring(0, Math.min(200, value.length())));
                    }
                } catch (Exception e) {
                    LOG.error("处理CDC事件失败: {}",
                            value.substring(0, Math.min(200, value.length())), e);
                }
            }
        }).name("CDC数据处理器");
    }

    /**
     * 为每个表创建StarRocks Sink
     */
    private static void createStarRocksSinks(
            SingleOutputStreamOperator<String> mainStream,
            Map<String, OutputTag<String>> tableTags,
            SyncConfig config) {

        LOG.info("创建StarRocks Sink...");

        SinkConfig sink = config.getSink();
        String targetDatabase = config.getSource().getTables().split("\\.")[0];

        for (Map.Entry<String, OutputTag<String>> entry : tableTags.entrySet()) {
            String fullTableName = entry.getKey();
            OutputTag<String> outputTag = entry.getValue();
            String tableName = fullTableName.split("\\.")[1];

            LOG.info("配置表 [{}] 的StarRocks Sink", fullTableName);

            // 创建Sink配置
            StarRocksSinkOptions sinkOptions = StarRocksSinkOptions.builder()
                    .withProperty("jdbc-url", sink.getJdbcUrl())
                    .withProperty("load-url", sink.getLoadUrl())
                    .withProperty("username", sink.getUsername())
                    .withProperty("password", sink.getPassword())
                    .withProperty("database-name", targetDatabase)
                    .withProperty("table-name", tableName)
                    .withProperty("sink.properties.format", "json")
                    .withProperty("sink.properties.strip_outer_array", "true")
                    .withProperty("sink.buffer-flush.max-bytes",
                            String.valueOf(sink.getSinkBufferFlushMaxBytes() != null ?
                                    sink.getSinkBufferFlushMaxBytes() : 1048576)) // 1MB
                    .withProperty("sink.buffer-flush.interval-ms",
                            String.valueOf(sink.getSinkBufferFlushIntervalMs() != null ?
                                    sink.getSinkBufferFlushIntervalMs() : 1000)) // 1秒
                    .withProperty("sink.buffer-flush.max-rows",
                            String.valueOf(sink.getSinkBufferFlushMaxRows() != null ?
                                    sink.getSinkBufferFlushMaxRows() : 100)) // 100行
                    .withProperty("table-create.properties.replication_num",
                            sink.getTableCreateProperties().getOrDefault("replication_num", "3"))
                    .build();

            // 绑定Sink
            mainStream.getSideOutput(outputTag)
                    .addSink(StarRocksSink.sink(sinkOptions))
                    .name("StarRocks Sink - " + tableName)
                    .setParallelism(config.getPipeline().getParallelism());
        }

        LOG.info("StarRocks Sink创建完成");
    }

    /**
     * 注册关闭钩子，优雅退出
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("接收到关闭信号，开始优雅退出...");
            isRunning.set(false);

            // 可以在这里添加资源清理逻辑
            LOG.info("同步程序已关闭");
        }));
    }
}
