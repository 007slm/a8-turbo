package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.dto.QueryDetailResponse;
import org.openjdbcproxy.cache.dto.QuerySummary;
import org.openjdbcproxy.cache.service.QueryManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
public class QueryController {

    private final QueryManagementService queryManagementService;

    @GetMapping("/list")
    @Operation(summary = "获取查询列表", description = "按数据库分组获取所有查询的摘要信息")
    public Map<String, List<QuerySummary>> getQueryList() {
        return queryManagementService.getQueryList();
    }

    @GetMapping("/{queryId}/details")
    @Operation(summary = "获取查询详情", description = "根据查询ID获取详细信息（含关联信息）")
    @SneakyThrows
    public QueryDetailResponse getQueryDetail(
            @Parameter(description = "查询ID") @PathVariable String queryId) {
        
        return queryManagementService.getQueryDetail(queryId);
    }


}