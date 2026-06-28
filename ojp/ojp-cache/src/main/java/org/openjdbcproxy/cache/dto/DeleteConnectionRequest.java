package org.openjdbcproxy.cache.dto;

import lombok.Data;

/**
 * DTO for deleting a connection configuration
 */
@Data
public class DeleteConnectionRequest {
    private String id;
}
