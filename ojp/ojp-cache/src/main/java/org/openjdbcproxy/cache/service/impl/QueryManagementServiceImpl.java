package org.openjdbcproxy.cache.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.entity.Query;
import org.openjdbcproxy.cache.dto.QueryDetailResponse;
import org.openjdbcproxy.cache.dto.QuerySummary;
import org.openjdbcproxy.cache.repository.QueryRepository;
import org.openjdbcproxy.cache.service.QueryManagementService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询管理服务实现类
 * 严格按照设计文档要求：去除分页参数，全量返回，按数据库分组
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryManagementServiceImpl implements QueryManagementService {

    private final QueryRepository queryRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    @SneakyThrows
    public Map<String, List<QuerySummary>> getQueryList() {
        List<QuerySummary> allSummaries = queryRepository.findAllSummaries(0, Integer.MAX_VALUE);
        return allSummaries.stream()
                .collect(Collectors.groupingBy(QuerySummary::getDatasourceName));
    }

    @Override
    @SneakyThrows
    public QueryDetailResponse getQueryDetail(String queryId) {
        return queryRepository.findById(queryId)
                .map(this::convertToDetailResponse)
                .orElse(null);
    }

    private QueryDetailResponse convertToDetailResponse(Query query) {
        return QueryDetailResponse.builder()
                .queryId(query.getQueryId())
                .sql(query.getSql())
                .datasourceName(query.getDatasourceName())
                .accessCount(query.getAccessCount())
                .cacheHitCount(query.getCacheHitCount())
                .cacheHitRate(query.getCacheHitRate())
                .avgResponseTime(query.getAvgResponseTime())
                .lastAccessTime(query.getLastAccessTime())
                .build();
    }
}