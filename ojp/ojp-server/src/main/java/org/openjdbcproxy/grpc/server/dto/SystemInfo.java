package org.openjdbcproxy.grpc.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 系统信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {
    
    /**
     * 应用名称
     */
    private String applicationName;
    
    /**
     * 版本
     */
    private String version;
    
    /**
     * Java版本
     */
    private String javaVersion;
    
    /**
     * Java供应商
     */
    private String javaVendor;
    
    /**
     * 操作系统名称
     */
    private String osName;
    
    /**
     * 操作系统版本
     */
    private String osVersion;
    
    /**
     * 操作系统架构
     */
    private String osArch;
    
    /**
     * 用户时区
     */
    private String userTimezone;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 启动时间
     */
    private Long startTime;
    
    /**
     * 运行时间
     */
    private Long uptime;
    
    /**
     * 输入参数
     */
    private List<String> inputArguments;
    
    /**
     * 系统属性
     */
    private Map<String, String> systemProperties;
}
