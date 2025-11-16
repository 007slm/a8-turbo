package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.cache.config.SeatunnelJobProperties;
import org.openjdbcproxy.cache.entity.CacheRule;
import org.openjdbcproxy.cache.entity.SlowQuery;
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

/**
 * Submits and cancels Seatunnel jobs on demand in response to cache rule mutations.
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

    private RestClient restClient;

    /**
     * Synchronise Seatunnel jobs to match the latest rule definition.
     *
     * @return an updated mapping of normalised table names to Seatunnel job ids.
     */
    public Map<String, String> synchroniseRule(CacheRule newRule, @Nullable CacheRule previousRule) {
        Map<String, String> currentJobIds =
                new HashMap<>(Optional.ofNullable(newRule.getSeatunnelJobIds()).orElseGet(HashMap::new));

        if (!properties.isEnabled()) {
            return currentJobIds;
        }
        Optional<MysqlEndpoint> targetEndpoint = parseEndpoint(newRule.getConnHash());
        if (targetEndpoint.isEmpty()) {
            log.warn("Skip Seatunnel sync for rule {}: unable to parse MySQL endpoint from connHash {}", newRule.getId(), newRule.getConnHash());
            cancelJobs(currentJobIds.values());
            return Collections.emptyMap();
        }

        MysqlEndpoint endpoint = targetEndpoint.get();
        Map<String, String> normalisedToActual = resolveTables(newRule);
        Set<String> newTables = new HashSet<>(normalisedToActual.keySet());

        Map<String, String> previousTables = resolveTables(previousRule);
        boolean connectionChanged = previousRule != null && !Objects.equals(
                parseEndpoint(previousRule.getConnHash()).orElse(null),
                endpoint);

        if (!newRule.isEnabled()) {
            log.info("Rule {} disabled, cancelling {} Seatunnel jobs", newRule.getId(), currentJobIds.size());
            cancelJobs(currentJobIds.values());
            return Collections.emptyMap();
        }

        if (connectionChanged) {
            log.info("Rule {} connection changed, cancelling {} existing jobs", newRule.getId(), currentJobIds.size());
            cancelJobs(currentJobIds.values());
            currentJobIds.clear();
            previousTables = Collections.emptyMap();
        }

        Set<String> removedTables = new HashSet<>(previousTables.keySet());
        removedTables.removeAll(newTables);
        for (String removed : removedTables) {
            String jobId = currentJobIds.remove(removed);
            if (jobId != null) {
                cancelJob(jobId);
            }
        }

        Map<String, String> resultingJobIds = new HashMap<>();
        for (Map.Entry<String, String> entry : normalisedToActual.entrySet()) {
            String normalised = entry.getKey();
            String actualTable = entry.getValue();
            String existingJob = currentJobIds.get(normalised);
            if (existingJob != null) {
                Optional<String> liveJob = findExistingJob(buildJobName(newRule.getId(), endpoint.database(), actualTable));
                if (liveJob.isPresent()) {
                    resultingJobIds.put(normalised, liveJob.get());
                    continue;
                }
            }
            submitSingleTableJob(newRule, endpoint, actualTable)
                    .ifPresent(jobId -> resultingJobIds.put(normalised, jobId));
        }

        return resultingJobIds;
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

    /**
     * Cancel all jobs associated with the provided rule.
     */
    public void removeRule(CacheRule rule) {
        Map<String, String> jobIds = Optional.ofNullable(rule.getSeatunnelJobIds()).orElse(Collections.emptyMap());
        if (jobIds.isEmpty()) {
            return;
        }
        log.info("Cancelling {} Seatunnel jobs for deleted rule {}", jobIds.size(), rule.getId());
        cancelJobs(jobIds.values());
    }

    private Optional<String> submitSingleTableJob(CacheRule rule,
                                                MysqlEndpoint endpoint,
                                                String tableName) {
        ensureRestClient();
        String database = endpoint.database();
        String jobName = buildJobName(rule.getId(), database, tableName);
        Optional<String> existing = findExistingJob(jobName);
        if (existing.isPresent()) {
            String jobId = existing.get();
            log.info("检测到已有同名 Seatunnel job: name={}, jobId={}, 跳过重复提交", jobName, jobId);
            inFlightJobs.put(jobName, jobId);
            return Optional.of(jobId);
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
                log.info("提交 Seatunnel job {} for rule {} table {} (jobId={})",
                        jobName, rule.getId(), tableName, jobId);
                inFlightJobs.put(jobName, jobId);
                return Optional.of(jobId);
            }
            log.warn("提交  job {} returned empty response", jobName);
        } catch (Exception ex) {
            log.error("提交 Seatunnel job {} for rule {}: {} 失败", jobName, rule.getId(), ex.getMessage(), ex);
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
                "isStopWithSavePoint", Boolean.FALSE
        );
        try {
            restClient()
                    .post()
                    .uri(cancelUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Cancelled Seatunnel job {}", jobId);
        } catch (Exception ex) {
            log.warn("Failed to cancel Seatunnel job {}: {}", jobId, ex.getMessage());
        } finally {
            inFlightJobs.values().removeIf(value -> Objects.equals(value, jobId));
        }
    }

    private Optional<String> findExistingJob(String jobName) {
        try {
            Object response = restClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.getListPath())
                            .build())
                    .retrieve()
                    .body(Object.class);
            return extractJobIdByName(response, jobName);
        } catch (Exception ex) {
            log.debug("查询 Seatunnel 已有作业失败，忽略重复检测: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractJobIdByName(Object payload, String jobName) {
        if (payload == null) {
            return Optional.empty();
        }
        if (payload instanceof Map<?, ?> map) {
            Object jobs = map.get("jobs");
            if (jobs instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    Optional<String> found = extractJobIdByName(item, jobName);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
            Object nameValue = map.get("jobName");
            if (jobName.equals(nameValue)) {
                Object id = map.get("jobId");
                return Optional.ofNullable(id == null ? null : String.valueOf(id));
            }
        } else if (payload instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Optional<String> found = extractJobIdByName(item, jobName);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private String buildJobRequest(String jobName,
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
        sb.append("    table-names = [\"").append(endpoint.database()).append(".").append(tableName).append("\"]\n");
        sb.append("    server-time-zone = \"").append(properties.getMysqlServerTimeZone()).append("\"\n");
        sb.append("    snapshot.split.size = ").append(properties.getMysqlSnapshotSplitSize()).append("\n");
        sb.append("    scan.startup.mode = \"initial\"\n");
        sb.append("    schema-changes.enabled = true\n");
        sb.append("    base-url = \"").append(endpoint.baseJdbcUrl()).append("\"\n");
        sb.append("  }\n");
        sb.append("}\n\n");

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
        sb.append("    table = \"").append(tableName).append("\"\n");
        properties.getSinkProperties().forEach(
                (key, value) -> sb.append("    sink.properties.").append(key).append(" = \"").append(value).append("\"\n")
        );
        sb.append("    sink.create_table.enable = true\n");
        sb.append("  }\n");
        sb.append("}\n");
        log.info("job内容");
        log.info(sb.toString());
        return sb.toString();
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

    private String buildJobName(String ruleId, String database, String table) {
        String safeRuleId = StringUtils.defaultIfBlank(ruleId, "rule");
        String slug = (database + "-" + table).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        String ruleSegment = safeRuleId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (ruleSegment.length() > 12) {
            ruleSegment = ruleSegment.substring(0, 12);
        }
        return "ojp-cache-" + ruleSegment + "-" + slug;
    }

    private int computeServerId(String identifier) {
        int hash = Math.abs(identifier.hashCode());
        return properties.getMysqlServerIdBase() + (hash % 1000);
    }

    private Optional<MysqlEndpoint> parseEndpoint(String connHash) {
        if (StringUtils.isBlank(connHash)) {
            return Optional.empty();
        }
        String trimmed = connHash.trim();
        int mysqlIdx = StringUtils.indexOf(trimmed, "mysql://");
        if (mysqlIdx < 0) {
            return Optional.empty();
        }
        String mysqlSection = trimmed.substring(mysqlIdx);
        try {
            URI uri = new URI(mysqlSection);
            String host = uri.getHost();
            if (StringUtils.isBlank(host)) {
                log.warn("Missing host information in connHash {}", connHash);
                return Optional.empty();
            }
            int port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_MYSQL_PORT;

            JdbcUrlUtil.Credentials credentials = JdbcUrlUtil.extractCredentials(uri);
            if (credentials.username() == null || credentials.password() == null) {
                log.warn("Missing MySQL credentials in connHash {} – expected user info or query parameters", connHash);
                return Optional.empty();
            }

            String path = uri.getPath();
            if (StringUtils.isBlank(path) || path.length() <= 1) {
                log.warn("Missing database name in connHash {}", connHash);
                return Optional.empty();
            }
            String database = path.substring(1);
            int queryIndex = database.indexOf('?');
            if (queryIndex >= 0) {
                database = database.substring(0, queryIndex);
            }

            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            String baseUrl = String.format("jdbc:mysql://%s:%d", host, port);
            return Optional.of(new MysqlEndpoint(host, port, database, credentials.username(), credentials.password(), jdbcUrl, baseUrl));
        } catch (URISyntaxException e) {
            log.warn("Failed to parse MySQL endpoint from connHash {}: {}", connHash, e.getMessage());
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

    private record MysqlEndpoint(String host, int port, String database, String username, String password,
                                 String jdbcUrl, String baseJdbcUrl) {
    }
}
