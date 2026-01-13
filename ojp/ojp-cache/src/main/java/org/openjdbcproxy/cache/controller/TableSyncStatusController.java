package org.openjdbcproxy.cache.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.TableSyncStatusDTO;
import org.openjdbcproxy.cache.monitor.TableSyncStateManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
}
