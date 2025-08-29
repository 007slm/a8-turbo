package org.openjdbcproxy.grpc.server.smartcache.serialization;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Represents metadata for a result set column
 */
@Data
@Builder
public class ColumnMetadata implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Column name
     */
    private String name;
    
    /**
     * Column label (display name)
     */
    private String label;
    
    /**
     * SQL type (from java.sql.Types)
     */
    private int type;
    
    /**
     * Database-specific type name
     */
    private String typeName;
    
    /**
     * Column precision
     */
    private int precision;
    
    /**
     * Column scale
     */
    private int scale;
    
    /**
     * Whether the column is nullable
     */
    private int nullable;
}