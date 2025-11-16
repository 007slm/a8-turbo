package org.openjdbcproxy.cache.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 分页慢查询响应。
 */
@Value
@Builder
public class SlowQueryPageResponse {
    List<SlowQuerySummary> items;
    long total;
    int page;
    int size;
    long totalPages;
}
