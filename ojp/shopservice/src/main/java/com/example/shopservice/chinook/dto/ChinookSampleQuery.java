package com.example.shopservice.chinook.dto;

/**
 * Predefined Chinook sample query metadata.
 *
 * @param id          unique identifier used by the UI
 * @param title       short description
 * @param description longer explanation of what the query demonstrates
 * @param sql         query text
 */
public record ChinookSampleQuery(
        String id,
        String title,
        String description,
        String sql
) {
}
