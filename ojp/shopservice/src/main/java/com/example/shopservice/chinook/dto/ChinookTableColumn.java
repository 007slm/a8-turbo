package com.example.shopservice.chinook.dto;

/**
 * Column definition for a Chinook table.
 *
 * @param ordinalPosition zero-based column order from metadata
 * @param name         column name
 * @param dataType     logical data type
 * @param nullable     whether NULL is allowed
 * @param defaultValue default value defined at schema level
 * @param comment      optional column comment/description
 */
public record ChinookTableColumn(
        int ordinalPosition,
        String name,
        String dataType,
        boolean nullable,
        String defaultValue,
        String comment
) {
}
