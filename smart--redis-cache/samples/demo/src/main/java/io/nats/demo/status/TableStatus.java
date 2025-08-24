package io.nats.demo.status;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * 表状态类
 * 用于跟踪和管理单个表的CDC同步状态
 */
@Data
@RequiredArgsConstructor
public class TableStatus {
    private SyncStatus status = SyncStatus.INITIALIZED;

    @NotNull
    private final String tableName;

    @NotNull
    private final Instant timestamp;

    /**
     * 表同步状态枚举
     *
     * 初始化完成
     * 监控到数据变化
     * 数据同步中
     * 数据同步成功
     * 数据同步失败
     */
    public enum SyncStatus {
        INITIALIZED,
        CHANGED,
        SYNCING,
        SUCCESS,
        FAILURE
    }


    public boolean isSyncFinished(){
        return this.status.equals(SyncStatus.SUCCESS);
    }



}