package org.openjdbcproxy.cache.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.TableSyncStatusDTO;
import org.openjdbcproxy.cache.monitor.TableSyncStateManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class TableSyncStatusController {

    private final TableSyncStateManager tableSyncStateManager;

    @GetMapping("/sync-states")
    public List<TableSyncStatusDTO> getSyncStates() {
        return tableSyncStateManager.getAllSyncStates().stream()
                .map(TableSyncStatusDTO::fromState)
                .collect(Collectors.toList());
    }

    @PostMapping("/events")
    public void receiveWebhookEvent(@RequestBody Map<String, String> payload) {
        log.info("OJP Server 收到来自 SeaTunnel 物理 Webhook 的事件: {}", payload);
        String eventType = payload.get("event");
        
        if ("SNAPSHOT_FINISHED".equals(eventType)) {
            String tables = payload.get("tables");
            log.info("全量同步完成事件已捕获，正在使相关表就绪: {}", tables);
            
            // 构造模拟事件并调用状态管理器更新状态
            // 在 SeaTunnel 体系中，jobName 通常是 buildJobName(connHash, database, table) 格式的 key
            // 我们需要提取表名或以 jobName 作为 Key 写入
            if (tables != null) {
                // 由于 tableIds 类似于 [test_db.test_table] 的格式，我们做一个规约解析或者是模糊匹配键值
                // 如果没有 jobName 字段，我们可以调用 tableSyncStateManager 里的 getAllSyncStates，
                // 找出其中 tableName 匹配的所有 TableSyncState 条目，并直接将它们的状态置为 INCREMENTAL_SYNCING！
                // 这比依赖 jobName 匹配更加彻底和鲁棒！
                String cleanTableStr = tables.replace("[", "").replace("]", "").trim();
                for (String singleTablePath : cleanTableStr.split(",")) {
                    String[] parts = singleTablePath.trim().split("\\.");
                    String tableNameOnly = parts[parts.length - 1].trim().toLowerCase(Locale.ROOT);
                    
                    tableSyncStateManager.getAllSyncStates().stream()
                        .filter(state -> tableNameOnly.equals(state.getTableName()))
                        .forEach(state -> {
                            Map<String, String> mockEvent = new HashMap<>();
                            mockEvent.put("type", "CDC_STATE_CHANGE");
                            mockEvent.put("jobName", state.getTableKey());
                            mockEvent.put("jobId", payload.getOrDefault("jobId", ""));
                            mockEvent.put("state", "INCREMENTAL_SYNCING");
                            mockEvent.put("timestamp", String.valueOf(System.currentTimeMillis()));
                            
                            tableSyncStateManager.updateFromStreamEvent(mockEvent);
                            log.info("成功利用物理 Webhook 将表 [{}] 的缓存状态强制更新为就绪 (INCREMENTAL_SYNCING)！", state.getTableKey());
                        });
                }
            }
        } else if ("INCREMENTAL_BATCH_COMMITTED".equals(eventType)) {
            // 增量物理数据落盘完成
            log.info("增量数据已成功提交物理落盘于 StarRocks 中！事件源: {}", payload.get("writer"));
        }
    }
}
