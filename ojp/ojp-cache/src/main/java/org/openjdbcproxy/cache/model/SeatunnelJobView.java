package org.openjdbcproxy.cache.model;

/**
 * Lightweight view model for Seatunnel job metadata used by UI responses.
 */
public record SeatunnelJobView(String table,
                               String normalizedTable,
                               String jobName,
                               String jobId,
                               String liveJobId,
                               String status) {
}
