package com.example.shopservice.chinook.dto;

/**
 * Column metadata for Chinook query result sets.
 *
 * @param name     column label returned by JDBC
 * @param type     vendor specific column type
 * @param nullable whether the column accepts null values
 */
public record ChinookColumnMetadata(
        String name,
        String type,
        boolean nullable
) {
}
