package com.example.shopservice.chinook.dto;

import java.util.List;

/**
 * Table metadata with column details.
 *
 * @param name    table name
 * @param columns ordered list of column definitions
 */
public record ChinookTable(
        String name,
        List<ChinookTableColumn> columns
) {
}
