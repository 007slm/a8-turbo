package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 系统健康信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthInfo {
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * JVM健康状态
     */
    private Map<String, Object> jvm;
    
    /**
     * 内存健康状态
     */
    private Map<String, Object> memory;
    
    /**
     * 系统健康状态
     */
    private Map<String, Object> system;
}
