package org.openjdbcproxy.cache.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seatunnel job configuration properties used when synthesising per-rule CDC pipelines.
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "ojp.cache.seatunnel")
public class SeatunnelJobProperties {

    /**
     * Controls whether dynamic Seatunnel job orchestration is enabled.
     */
    private boolean enabled = true;

    /**
     * Base URL of the Seatunnel master REST endpoint (e.g. http://seatunnel-master:8080).
     */
    private String masterBaseUrl = "http://seatunnel-master:8080";

    /**
     * Path used when submitting jobs via REST.
     */
    private String submitPath = "/submit-job";

    private String stopPath = "/stop-job";
    /**
     * Path used to list existing jobs (best-effort; response parsed client-side).
     */
    private String listPath = "/running-jobs";

    /**
     * Snapshot split size for the CDC connector.
     */
    private int mysqlSnapshotSplitSize = 8096;

    /**
     * Server id base value used when generating per-job MySQL server ids.
     */
    private int mysqlServerIdBase = 5400;

    /**
     * Server timezone for the source database.
     */
    private String mysqlServerTimeZone = "UTC";

    /**
     * StarRocks HTTP node URLs.
     */
    private List<String> starrocksNodeUrls = new ArrayList<>(List.of("starrocks:8040"));

    /**
     * StarRocks JDBC base URL.
     */
    private String starrocksBaseUrl = "jdbc:mysql://starrocks:9030";

    private String starrocksUsername = "root";

    private String starrocksPassword = "";

    /**
     * Additional sink properties forwarded to StarRocks stream load.
     */
    private Map<String, String> sinkProperties = new LinkedHashMap<>(
            Map.of(
                    "format", "json",
                    "strip_outer_array", "true",
                    "ignore_json_size", "true",
                    "sink.buffer-flush.interval.ms", "1000",
                    "sink.enable-upsert-delete", "true"
            )
    );

    /**
     * Default job parallelism.
     */
    private int parallelism = 1;

    /**
     * Default checkpoint interval (ms) for streaming jobs.
     */
    private long checkpointInterval = 3_000L;

    /**
     * HTTP timeout used when interacting with Seatunnel master.
     */
    /**
     * HTTP timeout used when interacting with Seatunnel master.
     */
    private Duration requestTimeout = Duration.ofSeconds(15);

    /**
     * Default Oracle port.
     */
    private int oracleDefaultPort = 1521;

    /**
     * Oracle LogMiner strategy (e.g., online_catalog, redo_log_catalog).
     */
    private String oracleLogMiningStrategy = "online_catalog";
}
