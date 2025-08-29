package org.openjdbcproxy.grpc.server.smartcache.serialization;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Represents cached query result data
 */
@Data
@Builder
public class CachedResultData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Column metadata information
     */
    private List<ColumnMetadata> columns;
    
    /**
     * Row data
     */
    private List<List<Object>> rows;
    
    /**
     * Total number of rows
     */
    private int rowCount;
}

