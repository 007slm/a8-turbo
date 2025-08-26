package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统健康状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthInfo {
    
    /**
     * 健康状态 (UP/DOWN/ERROR)
     */
    private String status;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * JVM健康状态
     */
    private JvmHealthInfo jvm;
    
    /**
     * 内存健康状态
     */
    private MemoryHealthInfo memory;
    
    /**
     * 系统健康状态
     */
    private SystemHealthInfo system;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
    
    /**
     * JVM健康状态信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmHealthInfo {
        private String status;
        private Long uptime;
        private Long startTime;
        private Integer threadCount;
        private Integer loadedClassCount;
        private Double heapUsagePercent;
        private String heapStatus;
        private String error;
    }
    
    /**
     * 内存健康状态信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryHealthInfo {
        private String status;
        private Long heapUsed;
        private Long heapMax;
        private Long nonHeapUsed;
        private Long nonHeapMax;
        private Double heapUsagePercent;
        private Double nonHeapUsagePercent;
        private String memoryStatus;
        private String error;
    }
    
    /**
     * 系统健康状态信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealthInfo {
        private String status;
        private Integer availableProcessors;
        private Long freeMemory;
        private Long totalMemory;
        private Long maxMemory;
        private Double memoryUsagePercent;
        private String memoryStatus;
        private String error;
    }
}
