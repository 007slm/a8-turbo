package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.controller.dto.SlowQueryFiltersResponse;
import org.openjdbcproxy.cache.controller.dto.SlowQueryPageResponse;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.service.SlowQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 查询管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/queries")
@Tag(name = "Query Management", description = "查询管理API")
@RequiredArgsConstructor
public class SlowQueryController {

    private final SlowQueryService slowQueryService;

    @GetMapping
    @Operation(summary = "分页获取慢查询", description = "支持连接、关键字、执行耗时、查询类型与表名过滤")
    public SlowQueryPageResponse getSlowQueries(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "connHash", required = false) String connHash,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minExecutionTime", required = false) Long minExecutionTime,
            @RequestParam(value = "queryType", required = false) String queryType,
            @RequestParam(value = "table", required = false) String tableName) {
        return slowQueryService.getSlowQueries(page, size, connHash, keyword, minExecutionTime, queryType, tableName);
    }

    @GetMapping("/{queryId}")
    @Operation(summary = "获取慢查询详情", description = "根据查询ID获取完整的慢查询信息")
    public ResponseEntity<SlowQuery> getSlowQueryDetail(@PathVariable(name = "queryId") String queryId) {
        return slowQueryService.findById(queryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/filters")
    @Operation(summary = "获取慢查询筛选项", description = "提供连接、查询类型、涉及表等筛选数据")
    public SlowQueryFiltersResponse getSlowQueryFilters() {
        return slowQueryService.getAvailableFilters();
    }

    @GetMapping("/list")
    @Operation(summary = "获取查询列表", description = "按数据库分组获取所有查询的摘要信息")
    public Map<String, List<SlowQuery>> getQueryList() {
        return slowQueryService.getQueriesGroupedByConnHash();
    }

    @GetMapping("/tables")
    @Operation(summary = "获取连接对应的表列表", description = "返回所有连接涉及的表名集合")
    public Map<String, List<String>> getTablesByConnHash() {
        return slowQueryService.getTablesByConnHash();
    }

    @GetMapping("/tables/{connHash}")
    @Operation(summary = "获取指定连接的表列表", description = "返回某个连接涉及的唯一表名集合")
    public List<String> getTablesForConnHash(@PathVariable String connHash) {
        return slowQueryService.getTablesForConnHash(connHash);
    }

}
