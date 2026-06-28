package org.openjdbcproxy.cache.controller.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 简要慢查询信息，供分页列表展示。
 */
@Value
@Builder
public class SlowQuerySummary {
    String id;
    String sql;
    String normalizedSql;
    String connHash;
    Long executionTime;
    Boolean hasError;
    Boolean inTransaction;
    String queryType;
    String tableNames;
    Long timestamp;
    String parameters;
    String queryAgeCategory; // 查询时效分类: "实时", "近期", "历史"
    Boolean isRecentQuery; // 是否为最近1小时内的查询
}
