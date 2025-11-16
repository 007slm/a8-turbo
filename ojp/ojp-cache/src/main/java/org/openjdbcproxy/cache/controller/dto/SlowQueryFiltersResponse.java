package org.openjdbcproxy.cache.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 慢查询可用筛选项响应。
 */
@Value
@Builder
public class SlowQueryFiltersResponse {
    List<String> connHashes;
    List<String> queryTypes;
    List<String> tables;
    long totalSlowQueries;
}
