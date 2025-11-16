package com.example.shopservice.chinook.dto;

import java.util.List;

/**
 * Response payload returned after executing a Chinook query.
 *
 * @param columns          ordered column metadata
 * @param rows             ordered row data aligned with {@code columns}
 * @param rowCount         number of rows returned (capped by {@code truncated})
 * @param truncated        whether the dataset was truncated due to row limit
 * @param executionTimeMs  query execution duration in milliseconds
 */
public record ChinookQueryResponse(
        List<ChinookColumnMetadata> columns,
        List<List<Object>> rows,
        int rowCount,
        boolean truncated,
        long executionTimeMs
) {
}
