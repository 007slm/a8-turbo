package org.openjdbcproxy.cache.dto;

import lombok.Data;

/**
 * DTO for updating CDC credentials
 */
@Data
public class UpdateCdcCredentialsRequest {
    private String id;
    private String cdcUsername;
    private String cdcPassword;
}
