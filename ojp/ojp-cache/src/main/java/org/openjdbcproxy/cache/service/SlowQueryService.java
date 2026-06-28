package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import org.openjdbcproxy.cache.controller.dto.SlowQueryFiltersResponse;
import org.openjdbcproxy.cache.controller.dto.SlowQueryPageResponse;
import org.openjdbcproxy.cache.controller.dto.SlowQuerySummary;
import org.openjdbcproxy.cache.entity.SlowQuery;
import org.openjdbcproxy.cache.repository.SlowQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 慢查询数据服务，提供分页、过滤与聚合能力。
 */
@Service
@RequiredArgsConstructor
public class SlowQueryService {

    private final SlowQueryRepository slowQueryRepository;

    public SlowQueryPageResponse getSlowQueries(int page,
            int size,
            String connHash,
            String keyword,
            Long minExecutionTime,
            String queryType,
            String tableName) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        List<SlowQuery> allQueries = slowQueryRepository.findAll();

        List<SlowQuery> filtered = allQueries.stream()
                .filter(query -> matchesConnHash(query, connHash))
                .filter(query -> matchesKeyword(query, keyword))
                .filter(query -> matchesExecutionTime(query, minExecutionTime))
                .filter(query -> matchesQueryType(query, queryType))
                .filter(query -> matchesTable(query, tableName))
                .sorted(Comparator.comparingLong(SlowQuery::getExecutionTime).reversed())
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.min((safePage - 1) * safeSize, (int) total);
        int toIndex = Math.min(fromIndex + safeSize, (int) total);

        List<SlowQuerySummary> items = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        long totalPages = total == 0 ? 0 : (long) Math.ceil((double) total / safeSize);

        return SlowQueryPageResponse.builder()
                .items(items)
                .total(total)
                .page(safePage)
                .size(safeSize)
                .totalPages(totalPages)
                .build();
    }

    public Optional<SlowQuery> findById(String queryId) {
        return slowQueryRepository.findById(queryId);
    }

    public SlowQueryFiltersResponse getAvailableFilters() {
        List<SlowQuery> queries = slowQueryRepository.findAll();

        Set<String> connHashes = new TreeSet<>();
        Set<String> queryTypes = new TreeSet<>();
        Set<String> tables = new TreeSet<>();

        for (SlowQuery query : queries) {
            if (StringUtils.hasText(query.getConnHash())) {
                connHashes.add(query.getConnHash());
            }
            if (StringUtils.hasText(query.getQueryType())) {
                queryTypes.add(query.getQueryType());
            }
            tables.addAll(parseTableNames(query.getTableNames()));
        }

        return SlowQueryFiltersResponse.builder()
                .connHashes(new ArrayList<>(connHashes))
                .queryTypes(new ArrayList<>(queryTypes))
                .tables(new ArrayList<>(tables))
                .totalSlowQueries(queries.size())
                .build();
    }

    public Map<String, List<SlowQuery>> getQueriesGroupedByConnHash() {
        return slowQueryRepository.findAll().stream()
                .collect(Collectors.groupingBy(SlowQuery::getConnHash));
    }

    public Map<String, List<String>> getTablesByConnHash() {
        Map<String, Set<String>> grouped = new HashMap<>();
        for (SlowQuery query : slowQueryRepository.findAll()) {
            String connHash = query.getConnHash();
            if (!StringUtils.hasText(connHash)) {
                continue;
            }
            grouped
                    .computeIfAbsent(connHash, key -> new TreeSet<>())
                    .addAll(parseTableNames(query.getTableNames()));
        }

        Map<String, List<String>> result = new HashMap<>();
        grouped.forEach((key, value) -> result.put(key, new ArrayList<>(value)));
        return result;
    }

    public List<String> getTablesForConnHash(String connHash) {
        if (!StringUtils.hasText(connHash)) {
            return Collections.emptyList();
        }
        return slowQueryRepository.findAll().stream()
                .filter(query -> connHash.equals(query.getConnHash()))
                .flatMap(query -> parseTableNames(query.getTableNames()).stream())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), ArrayList::new));
    }

    private boolean matchesConnHash(SlowQuery query, String connHash) {
        if (!StringUtils.hasText(connHash)) {
            return true;
        }
        return connHash.equals(query.getConnHash());
    }

    private boolean matchesKeyword(SlowQuery query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return Stream.of(query.getSql(), query.getNormalizedSql(), query.getParameters(), query.getMethodName())
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .anyMatch(value -> value.contains(lower));
    }

    private boolean matchesExecutionTime(SlowQuery query, Long minExecutionTime) {
        if (minExecutionTime == null || minExecutionTime <= 0) {
            return true;
        }
        return query.getExecutionTime() >= minExecutionTime;
    }

    private boolean matchesQueryType(SlowQuery query, String queryType) {
        if (!StringUtils.hasText(queryType)) {
            return true;
        }
        return queryType.equalsIgnoreCase(Optional.ofNullable(query.getQueryType()).orElse(""));
    }

    private boolean matchesTable(SlowQuery query, String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return true;
        }
        return parseTableNames(query.getTableNames()).stream()
                .anyMatch(name -> name.equalsIgnoreCase(tableName));
    }

    private SlowQuerySummary toSummary(SlowQuery query) {
        long currentTime = System.currentTimeMillis();
        long queryTime = query.getTimestamp();
        long ageInMinutes = (currentTime - queryTime) / (1000 * 60);

        // 计算查询时效分类
        String ageCategory;
        if (ageInMinutes < 5) {
            ageCategory = "实时";
        } else if (ageInMinutes < 60) {
            ageCategory = "近期";
        } else {
            ageCategory = "历史";
        }

        // 判断是否为最近1小时内的查询
        boolean isRecent = ageInMinutes < 60;

        return SlowQuerySummary.builder()
                .id(query.getId())
                .sql(query.getSql())
                .normalizedSql(query.getNormalizedSql())
                .connHash(query.getConnHash())
                .executionTime(query.getExecutionTime())
                .hasError(query.isHasError())
                .inTransaction(query.isInTransaction())
                .queryType(query.getQueryType())
                .tableNames(query.getTableNames())
                .timestamp(query.getTimestamp())
                .parameters(query.getParameters())
                .queryAgeCategory(ageCategory)
                .isRecentQuery(isRecent)
                .build();
    }

    private Set<String> parseTableNames(String tableNames) {
        if (!StringUtils.hasText(tableNames)) {
            return Collections.emptySet();
        }
        return Arrays.stream(tableNames.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
