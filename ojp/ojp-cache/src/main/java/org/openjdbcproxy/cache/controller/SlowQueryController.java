package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/queries")
@Tag(name = "Query Management", description = "查询管理API")
@RequiredArgsConstructor
public class SlowQueryController {

    private final SlowQueryRepository queryRepository;

    @GetMapping("/list")
    @Operation(summary = "获取查询列表", description = "按数据库分组获取所有查询的摘要信息")
    public Map<String, List<SlowQuery>> getQueryList() {
        List<SlowQuery> queries = queryRepository.findAll();

        Map<String, List<SlowQuery>> groupedQueries = queries.stream()
                .collect(Collectors.groupingBy(query -> query.getConnHash()));
        return groupedQueries;
    }

}