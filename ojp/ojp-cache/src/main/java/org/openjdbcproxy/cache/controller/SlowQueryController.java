package org.openjdbcproxy.cache.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
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

    @GetMapping("/tables")
    @Operation(summary = "获取连接对应的表列表", description = "返回所有连接涉及的表名集合")
    public Map<String, List<String>> getTablesByConnHash() {
        List<SlowQuery> queries = queryRepository.findAll();

        Map<String, Set<String>> groupedTables = new HashMap<>();

        for (SlowQuery query : queries) {
            String connHash = query.getConnHash();
            if (connHash == null || connHash.isEmpty()) {
                continue;
            }
            groupedTables
                    .computeIfAbsent(connHash, key -> new TreeSet<>())
                    .addAll(parseTableNames(query.getTableNames()));
        }

        Map<String, List<String>> result = new HashMap<>();
        groupedTables.forEach((key, value) -> result.put(key, new ArrayList<>(value)));
        return result;
    }

    @GetMapping("/tables/{connHash}")
    @Operation(summary = "获取指定连接的表列表", description = "返回某个连接涉及的唯一表名集合")
    public List<String> getTablesForConnHash(@PathVariable String connHash) {
        List<SlowQuery> queries = queryRepository.findAll();

        return queries.stream()
                .filter(query -> connHash.equals(query.getConnHash()))
                .flatMap(query -> parseTableNames(query.getTableNames()).stream())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), ArrayList::new));
    }

    private Set<String> parseTableNames(String tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(tableNames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

}
