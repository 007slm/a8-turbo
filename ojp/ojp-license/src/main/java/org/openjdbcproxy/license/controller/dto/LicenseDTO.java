package org.openjdbcproxy.license.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权信息数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseDTO {
    private String customer;
    private String expiryDate;
    private String licenseCode;
    private boolean valid;
    private String message;
}
