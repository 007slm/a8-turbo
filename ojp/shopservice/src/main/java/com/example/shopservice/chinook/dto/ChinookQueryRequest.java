package com.example.shopservice.chinook.dto;

/**
 * Request payload for executing ad-hoc Chinook SQL queries.
 *
 * @param sql     raw SQL text to execute (SELECT/WITH only)
 * @param maxRows optional maximum number of rows to return
 */
public record ChinookQueryRequest(
        String sql,
        Integer maxRows
) {
}
