package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.ApiResponse;
import org.openjdbcproxy.grpc.server.dto.HealthInfo;
import org.openjdbcproxy.grpc.server.dto.SystemInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统状态控制器
 * 提供系统状态相关的API端点
 */
@Slf4j
@RestController
@RequestMapping("/api/actuator")
@CrossOrigin(origins = "*")
public class SystemStatusController {
    
    @Value("${spring.application.name:ojp-server}")
    private String applicationName;
    
    @Value("${ojp.server.version:0.0.8-alpha}")
    private String serverVersion;
    
    /**
     * 1.1 系统健康状态
     * GET /api/actuator/health
     */
    @GetMapping("/health")
    public ApiResponse<HealthInfo> getHealth() {
        try {
            log.debug("Getting system health status");
            
            HealthInfo healthInfo = HealthInfo.builder()
                    .status("UP")
                    .timestamp(System.currentTimeMillis())
                    .jvm(checkJvmHealth())
                    .memory(checkMemoryHealth())
                    .system(checkSystemHealth())
                    .build();
            
            return ApiResponse.success(healthInfo, "获取系统健康状态成功");
        } catch (Exception e) {
            log.error("Failed to get system health status", e);
            return ApiResponse.error("HEALTH_CHECK_ERROR", "获取系统健康状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 1.2 系统信息
     * GET /api/actuator/info
     */
    @GetMapping("/info")
    public ApiResponse<SystemInfo> getSystemInfo() {
        try {
            log.debug("Getting system information");
            
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            SystemInfo systemInfo = SystemInfo.builder()
                    .applicationName(applicationName)
                    .version(serverVersion)
                    .javaVersion(System.getProperty("java.version"))
                    .javaVendor(System.getProperty("java.vendor"))
                    .osName(System.getProperty("os.name"))
                    .osVersion(System.getProperty("os.version"))
                    .osArch(System.getProperty("os.arch"))
                    .userTimezone(System.getProperty("user.timezone"))
                    .timestamp(System.currentTimeMillis())
                    .startTime(runtimeBean.getStartTime())
                    .uptime(runtimeBean.getUptime())
                    .inputArguments(runtimeBean.getInputArguments())
                    .systemProperties(runtimeBean.getSystemProperties())
                    .build();
            
            return ApiResponse.success(systemInfo, "获取系统信息成功");
        } catch (Exception e) {
            log.error("Failed to get system information", e);
            return ApiResponse.error("SYSTEM_INFO_ERROR", "获取系统信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 1.3 系统指标
     * GET /api/actuator/metrics
     */
    @GetMapping("/metrics")
    public ApiResponse<Map<String, Object>> getMetrics() {
        try {
            log.debug("Getting system metrics");
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("timestamp", System.currentTimeMillis());
            metrics.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
            metrics.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
            metrics.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
            
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            metrics.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage());
            metrics.put("nonHeapMemoryUsage", memoryBean.getNonHeapMemoryUsage());
            
            return ApiResponse.success(metrics, "获取系统指标成功");
        } catch (Exception e) {
            log.error("Failed to get system metrics", e);
            return ApiResponse.error("METRICS_ERROR", "获取系统指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查JVM健康状态
     */
    private Map<String, Object> checkJvmHealth() {
        Map<String, Object> jvmHealth = new HashMap<>();
        jvmHealth.put("status", "UP");
        jvmHealth.put("version", System.getProperty("java.version"));
        jvmHealth.put("vendor", System.getProperty("java.vendor"));
        return jvmHealth;
    }
    
    /**
     * 检查内存健康状态
     */
    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> memoryHealth = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        long maxHeap = memoryBean.getHeapMemoryUsage().getMax();
        long usedHeap = memoryBean.getHeapMemoryUsage().getUsed();
        double heapUsagePercent = maxHeap > 0 ? (double) usedHeap / maxHeap * 100 : 0;
        
        memoryHealth.put("status", heapUsagePercent < 90 ? "UP" : "WARN");
        memoryHealth.put("heapUsagePercent", Math.round(heapUsagePercent * 100.0) / 100.0);
        memoryHealth.put("maxHeap", maxHeap);
        memoryHealth.put("usedHeap", usedHeap);
        
        return memoryHealth;
    }
    
    /**
     * 检查系统健康状态
     */
    private Map<String, Object> checkSystemHealth() {
        Map<String, Object> systemHealth = new HashMap<>();
        systemHealth.put("status", "UP");
        systemHealth.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        systemHealth.put("arch", System.getProperty("os.arch"));
        systemHealth.put("processors", Runtime.getRuntime().availableProcessors());
        return systemHealth;
    }
}
