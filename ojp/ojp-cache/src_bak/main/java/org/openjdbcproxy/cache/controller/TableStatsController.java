package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.TableStatsResponse;
import org.openjdbcproxy.cache.dto.TableDetailResponse;
import org.openjdbcproxy.cache.service.TableStatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表格统计REST API控制器
 * 提供表格统计相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/stats/tables")
@Tag(name = "Table Statistics", description = "表格统计API")
@RequiredArgsConstructor
public class TableStatsController {

    private final TableStatsService tableStatsService;

    @GetMapping("/list")
    @Operation(summary = "获取表格统计列表", description = "按数据库分组获取所有表格的统计信息")
    public Map<String, List<TableStatsResponse>> getTableStatsList() {
        log.info("获取表格统计列表（按数据库分组）");
        Map<String, List<TableStatsResponse>> stats = tableStatsService.getTableStatsList();
        log.info("成功获取表格统计列表，数据库数量: {}", stats.size());
        return stats;
    }

    @GetMapping("/{tableName}/details")
    @Operation(summary = "获取表格详细统计", description = "获取指定表格的详细统计信息（含关联信息）")
    public TableDetailResponse getTableDetails(
            @Parameter(description = "表名") @PathVariable String tableName,
            @Parameter(description = "数据源名称") @RequestParam(required = false) String datasource) {
        log.info("获取表格详细统计，表名: {}, 数据源: {}", tableName, datasource);
        TableDetailResponse details = tableStatsService.getTableDetails(tableName, datasource);
        log.info("成功获取表格详细统计，表名: {}", tableName);
        return details;
    }
}