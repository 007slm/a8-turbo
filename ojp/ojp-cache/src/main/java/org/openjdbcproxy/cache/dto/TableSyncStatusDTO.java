package org.openjdbcproxy.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openjdbcproxy.cache.monitor.TableSyncState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSyncStatusDTO {
    private String connHash;
    private String database;
    private String tableName;

    // Status fields (Primary focus)
    private boolean isReady;
    private boolean isSnapshotFinished;
    private boolean isStale;
    private Long updateTime;

    // Supplemental Job Info
    private String jobName;
    private String jobId;
    private String ruleId;

    public static TableSyncStatusDTO fromState(TableSyncState state) {
        return TableSyncStatusDTO.builder()
                .connHash(state.getConnHash())
                .database(state.getDatabase())
                .tableName(state.getTableName())
                .isReady(state.isAvailable())
                .isSnapshotFinished(state.isSnapshotFinished())
                .isStale(state.isStale())
                .updateTime(state.getUpdateTime())
                .jobName(state.getTableKey()) // tableKey is the jobName
                .jobId(state.getJobId())
                .ruleId(state.getRuleId())
                .build();
    }
}
