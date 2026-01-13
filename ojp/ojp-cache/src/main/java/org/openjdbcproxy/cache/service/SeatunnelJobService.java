package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.cache.config.SeatunnelJobProperties;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.model.SeatunnelJobView;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Arrays;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Submits and cancels Seatunnel jobs on demand in response to cache rule
 * mutations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatunnelJobService {

    private static final int DEFAULT_MYSQL_PORT = 3306;

    private final SeatunnelJobProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final SlowQueryRepository slowQueryRepository;
    private final Map<String, String> inFlightJobs = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    private RestClient restClient;

    /**
     * Reconcile the state of all SeaTunnel jobs based on the provided list of
     * rules.
     * <p>
     * This method ensures that:
     * 1. Jobs required by at least one enabled rule are running (submitted if
     * missing).
     * 2. Jobs not required by any enabled rule are cancelled.
     * 3. Shared jobs (same DB + Table) are reused.
     *
     * @param rules All currently existing cache rules.
     * @return A map of JobName -> JobId for all currently active/valid jobs.
     */
    public Map<String, String> reconcile(List<CacheRule> rules) {
        if (!properties.isEnabled()) {
            return Collections.emptyMap();
        }

        // 1. Identify all expected jobs (unique by Job Name)
        // Map JobName -> Context (so we can submit if missing)
        Map<String, JobSubmissionContext> expectedJobs = new HashMap<>();

        for (CacheRule rule : rules) {
            if (!rule.isEnabled())
                continue;
            Optional<DatabaseEndpoint> endpointOpt = parseEndpoint(rule.getConnHash());
            if (endpointOpt.isEmpty())
                continue;
            DatabaseEndpoint endpoint = endpointOpt.get();

            Map<String, String> resolved = resolveTables(rule);
            for (Map.Entry<String, String> entry : resolved.entrySet()) {
                String actualTable = entry.getValue();
                String jobName = buildJobName(endpoint, actualTable);
                // We only need one valid request context for this job name
                expectedJobs.putIfAbsent(jobName, new JobSubmissionContext(rule, endpoint, actualTable));
            }
        }

        // 2. Get currently running jobs
        Map<String, String> runningJobs = listJobsByName();
        Map<String, String> finalActiveJobs = new HashMap<>();

        // 3. Submit Missing
        for (Map.Entry<String, JobSubmissionContext> entry : expectedJobs.entrySet()) {
            String jobName = entry.getKey();
            if (runningJobs.containsKey(jobName)) {
                finalActiveJobs.put(jobName, runningJobs.get(jobName));
            } else {
                JobSubmissionContext ctx = entry.getValue();
                // Pass checkExists=false since we already know it's missing from runningJobs
                submitSingleTableJob(ctx.rule, ctx.endpoint, ctx.table, false)
                        .ifPresent(jobId -> finalActiveJobs.put(jobName, jobId));
            }
        }

        // 4. Cancel Orphans
        List<String> toCancel = new ArrayList<>();
        for (Map.Entry<String, String> entry : runningJobs.entrySet()) {
            String jobName = entry.getKey();
            // Only manage jobs created by this service (prefix check)
            if (jobName.startsWith("ojp-cache-") && !expectedJobs.containsKey(jobName)) {
                toCancel.add(entry.getValue());
            }
        }

        if (!toCancel.isEmpty()) {
            log.info("同步: 正在取消 {} 个孤立作业...", toCancel.size());
            cancelJobs(toCancel);
        }

        return finalActiveJobs;
    }

    /**
     * Resolve the job IDs for a specific rule based on the global active jobs.
     */
    public Map<String, String> resolveJobIds(CacheRule rule, Map<String, String> globalActiveJobs) {
        if (!rule.isEnabled() || !properties.isEnabled()) {
            return Collections.emptyMap();
        }
        Optional<DatabaseEndpoint> endpointOpt = parseEndpoint(rule.getConnHash());
        if (endpointOpt.isEmpty()) {
            return Collections.emptyMap();
        }
        DatabaseEndpoint endpoint = endpointOpt.get();
        Map<String, String> ruleJobIds = new HashMap<>();
        Map<String, String> resolved = resolveTables(rule);

        for (Map.Entry<String, String> entry : resolved.entrySet()) {
            String normalised = entry.getKey();
            String actualTable = entry.getValue();
            String jobName = buildJobName(endpoint, actualTable);
            String jobId = globalActiveJobs.get(jobName);
            if (jobId != null) {
                ruleJobIds.put(normalised, jobId);
            }
        }
        return ruleJobIds;
    }

    /**
     * Describe Seatunnel jobs for a given rule so the UI can render status.
     */
    public List<SeatunnelJobView> describeRuleJobs(CacheRule rule) {
        Map<String, String> tables = new LinkedHashMap<>(resolveTables(rule));
        Map<String, String> storedJobIds = Optional.ofNullable(rule.getSeatunnelJobIds())
                .orElse(Collections.emptyMap());
        storedJobIds.forEach(tables::putIfAbsent);

        Map<String, String> runningJobsByName = properties.isEnabled() ? listJobsByName() : Collections.emptyMap();
        Optional<DatabaseEndpoint> endpoint = parseEndpoint(rule.getConnHash());
        String database = endpoint.map(DatabaseEndpoint::database).orElse(null);
        boolean hasEndpoint = endpoint.isPresent();

        List<SeatunnelJobView> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : tables.entrySet()) {
            String normalisedTable = entry.getKey();
            String actualTable = entry.getValue();
            String jobName = hasEndpoint ? buildJobName(endpoint.get(), actualTable) : null;
            String liveJobId = jobName == null ? null : runningJobsByName.get(jobName);
            String storedJobId = storedJobIds.get(normalisedTable);
            String status = deriveStatus(properties.isEnabled(), hasEndpoint, storedJobId, liveJobId);
            result.add(new SeatunnelJobView(actualTable, normalisedTable, jobName, storedJobId, liveJobId, status));
        }

        if (result.isEmpty() && !properties.isEnabled()) {
            result.add(new SeatunnelJobView(null, null, null, null, null, "disabled"));
        }

        return result;
    }

    private Map<String, String> resolveTables(@Nullable CacheRule rule) {
        if (rule == null) {
            return Collections.emptyMap();
        }

        Map<String, String> mapping = new LinkedHashMap<>(normaliseTables(rule.getTables()));
        Set<String> seen = new HashSet<>(mapping.keySet());

        List<String> slowQueryIds = Optional.ofNullable(rule.getSlowQueryIds()).orElse(Collections.emptyList());
        if (!slowQueryIds.isEmpty()) {
            Iterable<SlowQuery> slowQueries = slowQueryRepository.findAllById(slowQueryIds);
            for (SlowQuery slowQuery : slowQueries) {
                if (!Objects.equals(rule.getConnHash(), slowQuery.getConnHash())) {
                    continue;
                }
                for (String table : parseSlowQueryTables(slowQuery.getTableNames())) {
                    String normalised = normaliseTableName(table);
                    if (seen.add(normalised)) {
                        mapping.put(normalised, table);
                    }
                }
            }
        }

        return mapping;
    }

    private Optional<String> submitSingleTableJob(CacheRule rule,
            DatabaseEndpoint endpoint,
            String tableName) {
        return submitSingleTableJob(rule, endpoint, tableName, true);
    }

    private Optional<String> submitSingleTableJob(CacheRule rule,
            DatabaseEndpoint endpoint,
            String tableName,
            boolean checkExists) {
        ensureRestClient();
        String database = endpoint.database();
        String jobName = buildJobName(endpoint, tableName);

        if (checkExists) {
            Optional<String> existing = findExistingJob(jobName);
            if (existing.isPresent()) {
                String jobId = existing.get();
                log.info("检测到已有同名 Seatunnel job: name={}, jobId={}, 跳过重复提交", jobName, jobId);
                inFlightJobs.put(jobName, jobId);
                return Optional.of(jobId);
            }
        }

        String resultTableName = jobName.replace('-', '_');
        String jobConfig = buildJobRequest(jobName, resultTableName, endpoint, tableName);
        URI submitUri = UriComponentsBuilder
                .fromHttpUrl(properties.getMasterBaseUrl())
                .path(properties.getSubmitPath())
                .queryParam("format", "hocon")
                .queryParam("jobName", jobName)
                .build(true)
                .toUri();
        try {
            Map<String, String> response = restClient()
                    .post()
                    .uri(submitUri)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(jobConfig)
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("jobId") != null) {
                String jobId = response.get("jobId");
                log.info("提交 Seatunnel job {} (规则: {}, 表: {}), jobId={}",
                        jobName, rule.getId(), tableName, jobId);
                inFlightJobs.put(jobName, jobId);
                return Optional.of(jobId);
            }
            log.warn("提交 job {} 返回了空响应", jobName);
        } catch (Exception ex) {
            log.error("提交 Seatunnel job {} (规则: {}) 失败: {}", jobName, rule.getId(), ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    private void cancelJobs(Iterable<String> jobIds) {
        for (String jobId : jobIds) {
            cancelJob(jobId);
        }
    }

    private void cancelJob(String jobId) {
        if (jobId == null) {
            return;
        }
        ensureRestClient();
        URI cancelUri = UriComponentsBuilder
                .fromHttpUrl(properties.getMasterBaseUrl())
                .path(properties.getStopPath())
                .build(true)
                .toUri();
        Map<String, Object> payload = Map.of(
                "jobId", jobId,
                "isStopWithSavePoint", Boolean.FALSE);
        try {
            restClient()
                    .post()
                    .uri(cancelUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("已取消 Seatunnel job {}", jobId);
        } catch (Exception ex) {
            log.warn("取消 Seatunnel job {} 失败: {}", jobId, ex.getMessage());
        } finally {
            inFlightJobs.values().removeIf(value -> Objects.equals(value, jobId));
        }
    }

    private Optional<String> findExistingJob(String jobName) {
        try {
            Map<String, String> jobs = listJobsByName();
            return Optional.ofNullable(jobs.get(jobName));
        } catch (Exception ex) {
            log.debug("查询 Seatunnel 已有作业失败，忽略重复检测: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> listJobsByName() {
        try {
            Object response = restClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.getListPath())
                            .build())
                    .retrieve()
                    .body(Object.class);
            Map<String, String> jobs = new HashMap<>();
            collectJobsByName(response, jobs);
            return jobs;
        } catch (Exception ex) {
            log.debug("查询 Seatunnel 作业列表失败，忽略状态同步: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private void collectJobsByName(Object payload, Map<String, String> accumulator) {
        if (payload == null) {
            return;
        }
        if (payload instanceof Map<?, ?> map) {
            Object jobs = map.get("jobs");
            if (jobs instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    collectJobsByName(item, accumulator);
                }
            }
            Object nameValue = map.get("jobName");
            Object idValue = map.get("jobId");
            if (nameValue != null && idValue != null) {
                accumulator.put(String.valueOf(nameValue), String.valueOf(idValue));
            }
        } else if (payload instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                collectJobsByName(item, accumulator);
            }
        }
    }

    private String deriveStatus(boolean enabled, boolean hasEndpoint, String jobId, String liveJobId) {
        if (!enabled) {
            return "disabled";
        }
        if (!hasEndpoint) {
            return "invalid-connection";
        }
        if (StringUtils.isNotBlank(liveJobId)) {
            return "running";
        }
        if (StringUtils.isNotBlank(jobId)) {
            return "missing";
        }
        return "pending";
    }

    private String buildJobRequest(String jobName,
            String resultTableName,
            DatabaseEndpoint endpoint,
            String tableName) {
        if (endpoint instanceof MysqlEndpoint mysqlEndpoint) {
            return buildMysqlJobRequest(jobName, resultTableName, mysqlEndpoint, tableName);
        } else if (endpoint instanceof OracleEndpoint oracleEndpoint) {
            return buildOracleJobRequest(jobName, resultTableName, oracleEndpoint, tableName);
        }
        throw new IllegalArgumentException("Unsupported endpoint type: " + endpoint.getClass().getName());
    }

    private String buildMysqlJobRequest(String jobName,
            String resultTableName,
            MysqlEndpoint endpoint,
            String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("env {\n");
        sb.append("  execution.parallelism = ").append(properties.getParallelism()).append("\n");
        sb.append("  job.mode = \"STREAMING\"\n");
        sb.append("  checkpoint.interval = ").append(properties.getCheckpointInterval()).append("\n");
        sb.append("}\n\n");

        sb.append("source {\n");
        sb.append("  MySQL-CDC {\n");
        sb.append("    result_table_name = \"").append(resultTableName).append("\"\n");
        sb.append("    url = \"").append(endpoint.jdbcUrl()).append("\"\n");
        sb.append("    username = \"").append(endpoint.username()).append("\"\n");
        sb.append("    password = \"").append(endpoint.password()).append("\"\n");
        sb.append("    database-names = [\"").append(endpoint.database()).append("\"]\n");
        sb.append("    table-names = [\"").append(endpoint.database()).append(".")
                .append(tableName.toLowerCase(Locale.ROOT)).append("\"]\n");
        sb.append("    server-time-zone = \"").append(properties.getMysqlServerTimeZone()).append("\"\n");
        sb.append("    snapshot.split.size = ").append(properties.getMysqlSnapshotSplitSize()).append("\n");
        sb.append("    scan.startup.mode = \"initial\"\n");
        sb.append("    schema-changes.enabled = true\n");
        sb.append("    base-url = \"").append(endpoint.baseJdbcUrl()).append("\"\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        appendCommonTransformAndSink(sb, endpoint, resultTableName, tableName); // Changed to use resultTableName as
                                                                                // source for sink if needed, but strict
                                                                                // translation uses tableName in sink
                                                                                // logic
        return sb.toString();
    }

    private String buildOracleJobRequest(String jobName,
            String resultTableName,
            OracleEndpoint endpoint,
            String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("env {\n");
        sb.append("  execution.parallelism = ").append(properties.getParallelism()).append("\n");
        sb.append("  job.mode = \"STREAMING\"\n");
        sb.append("  checkpoint.interval = ").append(properties.getCheckpointInterval()).append("\n");
        sb.append("}\n\n");

        sb.append("source {\n");
        sb.append("  Oracle-CDC {\n");
        sb.append("    result_table_name = \"").append(resultTableName).append("\"\n");
        sb.append("    url = \"").append(endpoint.jdbcUrl()).append("\"\n");
        sb.append("    username = \"").append(endpoint.username()).append("\"\n");
        sb.append("    password = \"").append(endpoint.password()).append("\"\n");
        sb.append("    database-names = [\"").append(endpoint.serviceName()).append("\"]\n");
        sb.append("    schema-names = [\"").append(endpoint.schema()).append("\"]\n");
        sb.append("    table-names = [\"").append(endpoint.schema()).append(".").append(tableName).append("\"]\n");
        sb.append("    startup.mode = \"initial\"\n");
        sb.append("    debezium.log.mining.strategy = \"").append(properties.getOracleLogMiningStrategy())
                .append("\"\n");
        sb.append("    debezium.database.tablename.case.insensitive = false\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        appendCommonTransformAndSink(sb, endpoint, resultTableName, tableName);
        return sb.toString();
    }

    private void appendCommonTransformAndSink(StringBuilder sb, DatabaseEndpoint endpoint, String resultTableName,
            String tableName) {
        sb.append("transform {\n");
        sb.append("}\n\n");

        sb.append("sink {\n");
        sb.append("  StarRocks {\n");
        sb.append("    nodeUrls = [");
        List<String> nodeUrls = new ArrayList<>(properties.getStarrocksNodeUrls());
        for (int i = 0; i < nodeUrls.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(nodeUrls.get(i)).append("\"");
        }
        sb.append("]\n");
        sb.append("    base-url = \"").append(properties.getStarrocksBaseUrl()).append("\"\n");
        sb.append("    username = \"").append(properties.getStarrocksUsername()).append("\"\n");
        sb.append("    password = \"").append(properties.getStarrocksPassword()).append("\"\n");
        sb.append("    database = \"").append(endpoint.database()).append("\"\n");

        // Use lowercase table name for sink configuration (as requested for StarRocks)
        String sinkTableName = tableName;
        if (endpoint instanceof MysqlEndpoint) {
            sinkTableName = tableName.toLowerCase(Locale.ROOT);
        }
        sb.append("    table = \"").append(sinkTableName).append("\"\n");

        properties.getSinkProperties().forEach(
                (key, value) -> sb.append("    sink.properties.").append(key).append(" = \"").append(value)
                        .append("\"\n"));

        // Fetch PK info from Redis to determine template
        try {
            // Lookup using lowercase table name from the Hash structure
            String redisHashKey = "ojp:schema:" + endpoint.connHash() + ":pks";
            String fieldKey = tableName.toLowerCase(Locale.ROOT);
            Object pkObj = redisTemplate.opsForHash().get(redisHashKey, fieldKey);
            String pkStr = pkObj != null ? pkObj.toString() : null;

            if (pkStr != null) {
                String template;
                if (StringUtils.isBlank(pkStr)) {
                    // No PK -> Duplicate Key model with Random Distribution
                    template = "CREATE TABLE IF NOT EXISTS `${database}`.`${table}` ( " +
                            "${rowtype_fields} " +
                            ") ENGINE=OLAP " +
                            "DISTRIBUTED BY RANDOM " +
                            "PROPERTIES ( \\\"replication_num\\\" = \\\"1\\\" )";
                } else {
                    // PK exists -> Primary Key model with Hash Distribution
                    String pkList = Arrays.stream(pkStr.split(","))
                            .map(String::trim)
                            .map(s -> "\\\"" + s + "\\\"")
                            .collect(Collectors.joining(", "));
                    template = "CREATE TABLE IF NOT EXISTS `${database}`.`${table}` ( " +
                            "${rowtype_fields} " +
                            ") ENGINE=OLAP " +
                            "PRIMARY KEY (" + pkList + ") " +
                            "DISTRIBUTED BY HASH (" + pkList + ") " +
                            "PROPERTIES ( \\\"replication_num\\\" = \\\"1\\\" )";
                }
                sb.append("    sink.save_mode_create_template = \"").append(template).append("\"\n");
                log.info("为表 {} 应用了自定义 StarRocks 模板 (PKs: [{}])", tableName, pkStr);
            } else {
                log.warn("Redis 中未找到表 {} 的 schema 信息，使用 Seatunnel 默认配置", tableName);
            }
        } catch (Exception e) {
            log.error("查询 Redis schema 信息失败: {}", e.getMessage());
        }

        sb.append("    sink.create_table.enable = true\n");
        sb.append("  }\n");
        sb.append("}\n");
        log.info("已生成 job 内容 {}: \n{}", tableName, sb.toString());
    }

    private Map<String, String> normaliseTables(List<String> tables) {
        Map<String, String> mapping = new HashMap<>();
        if (tables == null) {
            return mapping;
        }
        for (String table : tables) {
            if (StringUtils.isBlank(table)) {
                continue;
            }
            mapping.put(normaliseTableName(table), table.trim());
        }
        return mapping;
    }

    private Set<String> parseSlowQueryTables(@Nullable String tableNames) {
        if (StringUtils.isBlank(tableNames)) {
            return Collections.emptySet();
        }
        String[] parts = tableNames.split(",");
        Set<String> tables = new LinkedHashSet<>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                tables.add(part.trim());
            }
        }
        return tables;
    }

    private String normaliseTableName(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String buildJobName(DatabaseEndpoint endpoint, String table) {
        return org.openjdbcproxy.cache.util.SeatunnelUtils.buildJobName(endpoint.connHash(), endpoint.database(),
                table);
    }

    private int computeServerId(String identifier) {
        int hash = Math.abs(identifier.hashCode());
        return properties.getMysqlServerIdBase() + (hash % 1000);
    }

    private Optional<DatabaseEndpoint> parseEndpoint(String connHash) {
        if (StringUtils.isBlank(connHash)) {
            return Optional.empty();
        }
        String trimmed = connHash.trim();

        // Oracle detection
        if (trimmed.contains("oracle://") || trimmed.contains("jdbc:oracle:")) {
            return parseOracleEndpoint(trimmed);
        }

        int mysqlIdx = StringUtils.indexOf(trimmed, "mysql://");
        if (mysqlIdx < 0) {
            return Optional.empty();
        }
        String mysqlSection = trimmed.substring(mysqlIdx);
        try {
            URI uri = new URI(mysqlSection);
            String host = uri.getHost();
            if (StringUtils.isBlank(host)) {
                log.warn("connHash {} 中缺失主机信息", connHash);
                return Optional.empty();
            }
            int port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_MYSQL_PORT;

            JdbcUrlUtil.Credentials credentials = JdbcUrlUtil.extractCredentials(uri);
            if (credentials.username() == null || credentials.password() == null) {
                log.warn("connHash {} 中缺失 MySQL 凭据 - 预期包含用户信息或查询参数", connHash);
                return Optional.empty();
            }

            String path = uri.getPath();
            if (StringUtils.isBlank(path) || path.length() <= 1) {
                log.warn("connHash {} 中缺失数据库名称", connHash);
                return Optional.empty();
            }
            String database = path.substring(1);
            int queryIndex = database.indexOf('?');
            if (queryIndex >= 0) {
                database = database.substring(0, queryIndex);
            }

            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            String baseUrl = String.format("jdbc:mysql://%s:%d", host, port);
            return Optional.of(new MysqlEndpoint(host, port, database, credentials.username(), credentials.password(),
                    jdbcUrl, baseUrl, connHash));
        } catch (URISyntaxException e) {
            log.warn("解析 MySQL 端点失败 (connHash: {}): {}", connHash, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<DatabaseEndpoint> parseOracleEndpoint(String connHash) {
        try {
            // Expecting format like oracle://user:pass@host:1521/serviceName?schema=SCHEMA
            // Or jdbc:oracle:thin:@host:1521/serviceName

            // Simplistic parsing for now, assuming standard URI structure if possible
            // However, oracle JDBC URLs are notoriously complex.
            // We'll try to parse the "oracle://" part as a URI.

            String uriString = connHash;
            if (!uriString.startsWith("oracle://") && !uriString.startsWith("jdbc:oracle:")) {
                return Optional.empty();
            }

            // Convert jdbc:oracle:thin:@host:port/service to oracle://host:port/service for
            // parsing if needed
            // But let's assume the user provides our agreed format:
            // oracle://user:pass@host:1521/serviceName?schema=SCHEMA

            if (uriString.startsWith("jdbc:oracle:")) {
                // Fallback or handle standard JDBC if it contains credentials
                // Standard JDBC usually doesn't have user/pass in URL for Oracle unless using
                // OCI
                log.warn("尚未完全实现原始 Oracle JDBC URL 解析，请使用包含用户信息的 oracle:// 协议");
                return Optional.empty();
            }

            URI uri = new URI(uriString);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : properties.getOracleDefaultPort();

            JdbcUrlUtil.Credentials credentials = JdbcUrlUtil.extractCredentials(uri);
            String username = credentials.username();
            String password = credentials.password();

            if (StringUtils.isBlank(host) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                log.warn("无效的 Oracle 连接字符串，缺失主机或凭据: {}", connHash);
                return Optional.empty();
            }

            String path = uri.getPath(); // /serviceName
            String serviceName = (path != null && path.length() > 1) ? path.substring(1) : "ORCL";

            // Query params for schema
            String query = uri.getQuery();
            String schema = username.toUpperCase(); // Default to username
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "schema".equalsIgnoreCase(pair[0])) {
                        schema = pair[1];
                    }
                }
            }

            String jdbcUrl = String.format("jdbc:oracle:thin:@%s:%d/%s", host, port, serviceName);
            return Optional
                    .of(new OracleEndpoint(host, port, serviceName, schema, username, password, jdbcUrl, connHash));

        } catch (Exception e) {
            log.warn("无法解析 Oracle 端点 {}: {}", connHash, e.getMessage());
            return Optional.empty();
        }
    }

    private void ensureRestClient() {
        if (this.restClient != null) {
            return;
        }
        Duration timeout = properties.getRequestTimeout();
        int timeoutMillis = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        this.restClient = restClientBuilder
                .baseUrl(properties.getMasterBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private RestClient restClient() {
        ensureRestClient();
        return this.restClient;
    }

    private sealed interface DatabaseEndpoint permits MysqlEndpoint, OracleEndpoint {
        String host();

        int port();

        String database();

        String username();

        String password();

        String jdbcUrl();

        String connHash();
    }

    private record MysqlEndpoint(String host, int port, String database, String username, String password,
            String jdbcUrl, String baseJdbcUrl, String connHash) implements DatabaseEndpoint {
    }

    private record OracleEndpoint(String host, int port, String serviceName, String schema, String username,
            String password,
            String jdbcUrl, String connHash) implements DatabaseEndpoint {
        @Override
        public String database() {
            return schema;
        }
    }

    private record JobSubmissionContext(CacheRule rule, DatabaseEndpoint endpoint, String table) {
    }

}
