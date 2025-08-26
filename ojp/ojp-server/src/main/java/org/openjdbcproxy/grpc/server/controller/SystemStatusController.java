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
            
            // JVM指标
            Map<String, Object> jvmMetrics = getJvmMetrics();
            metrics.put("jvm", jvmMetrics);
            
            // 内存指标
            Map<String, Object> memoryMetrics = getMemoryMetrics();
            metrics.put("memory", memoryMetrics);
            
            // 系统指标
            Map<String, Object> systemMetrics = getSystemMetrics();
            metrics.put("system", systemMetrics);
            
            // 应用指标
            Map<String, Object> applicationMetrics = getApplicationMetrics();
            metrics.put("application", applicationMetrics);
            
            return ApiResponse.success(metrics, "获取系统指标成功");
        } catch (Exception e) {
            log.error("Failed to get system metrics", e);
            return ApiResponse.error("METRICS_ERROR", "获取系统指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取特定指标
     * GET /api/actuator/metrics/{metricName}
     */
    @GetMapping("/metrics/{metricName}")
    public ApiResponse<Object> getSpecificMetric(@PathVariable String metricName) {
        try {
            log.debug("Getting specific metric: {}", metricName);
            
            Object metricValue = null;
            switch (metricName.toLowerCase()) {
                case "jvm":
                    metricValue = getJvmMetrics();
                    break;
                case "memory":
                    metricValue = getMemoryMetrics();
                    break;
                case "system":
                    metricValue = getSystemMetrics();
                    break;
                case "application":
                    metricValue = getApplicationMetrics();
                    break;
                case "uptime":
                    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
                    metricValue = runtimeBean.getUptime();
                    break;
                case "threads":
                    metricValue = ManagementFactory.getThreadMXBean().getThreadCount();
                    break;
                case "classes":
                    metricValue = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
                    break;
                default:
                    return ApiResponse.error("METRIC_NOT_FOUND", "指标不存在: " + metricName);
            }
            
            return ApiResponse.success(metricValue, "获取指标成功");
        } catch (Exception e) {
            log.error("Failed to get specific metric: {}", metricName, e);
            return ApiResponse.error("GET_METRIC_ERROR", "获取指标失败: " + e.getMessage());
        }
    }
    
    // 私有辅助方法
    
    private HealthInfo.JvmHealthInfo checkJvmHealth() {
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 检查内存使用情况
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
            
            return HealthInfo.JvmHealthInfo.builder()
                    .status("UP")
                    .uptime(runtimeBean.getUptime())
                    .startTime(runtimeBean.getStartTime())
                    .threadCount(ManagementFactory.getThreadMXBean().getThreadCount())
                    .loadedClassCount(ManagementFactory.getClassLoadingMXBean().getLoadedClassCount())
                    .heapUsagePercent(Math.round(heapUsagePercent * 100.0) / 100.0)
                    .heapStatus(heapUsagePercent > 90 ? "WARNING" : "OK")
                    .build();
            
        } catch (Exception e) {
            log.warn("Failed to check JVM health", e);
            return HealthInfo.JvmHealthInfo.builder()
                    .status("DOWN")
                    .error(e.getMessage())
                    .build();
        }
    }
    
    private HealthInfo.MemoryHealthInfo checkMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 堆内存
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
            double heapUsagePercent = heapMax > 0 ? Math.round((double) heapUsed / heapMax * 10000) / 100.0 : 0;
            
            // 非堆内存
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            long nonHeapCommitted = memoryBean.getNonHeapMemoryUsage().getCommitted();
            double nonHeapUsagePercent = nonHeapMax > 0 ? Math.round((double) nonHeapUsed / nonHeapMax * 10000) / 100.0 : 0;
            
            return HealthInfo.MemoryHealthInfo.builder()
                    .status("UP")
                    .heapUsed(heapUsed)
                    .heapMax(heapMax)
                    .nonHeapUsed(nonHeapUsed)
                    .nonHeapMax(nonHeapMax)
                    .heapUsagePercent(heapUsagePercent)
                    .nonHeapUsagePercent(nonHeapUsagePercent)
                    .memoryStatus(heapUsagePercent > 90 ? "WARNING" : "OK")
                    .build();
            
        } catch (Exception e) {
            log.warn("Failed to check memory health", e);
            return HealthInfo.MemoryHealthInfo.builder()
                    .status("DOWN")
                    .error(e.getMessage())
                    .build();
        }
    }
    
    private HealthInfo.SystemHealthInfo checkSystemHealth() {
        try {
            // 计算内存使用百分比
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0;
            
            return HealthInfo.SystemHealthInfo.builder()
                    .status("UP")
                    .availableProcessors(Runtime.getRuntime().availableProcessors())
                    .freeMemory(Runtime.getRuntime().freeMemory())
                    .totalMemory(Runtime.getRuntime().totalMemory())
                    .maxMemory(Runtime.getRuntime().maxMemory())
                    .memoryUsagePercent(Math.round(memoryUsagePercent * 100.0) / 100.0)
                    .memoryStatus(memoryUsagePercent > 90 ? "WARNING" : "OK")
                    .build();
            
        } catch (Exception e) {
            log.warn("Failed to check system health", e);
            return HealthInfo.SystemHealthInfo.builder()
                    .status("DOWN")
                    .error(e.getMessage())
                    .build();
        }
    }
    
    private Map<String, Object> getJvmMetrics() {
        Map<String, Object> jvmMetrics = new HashMap<>();
        
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            jvmMetrics.put("uptime", runtimeBean.getUptime());
            jvmMetrics.put("startTime", runtimeBean.getStartTime());
            jvmMetrics.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
            jvmMetrics.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
            jvmMetrics.put("daemonThreadCount", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
            jvmMetrics.put("loadedClassCount", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
            jvmMetrics.put("totalLoadedClassCount", ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
            jvmMetrics.put("unloadedClassCount", ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount());
            
        } catch (Exception e) {
            log.warn("Failed to get JVM metrics", e);
        }
        
        return jvmMetrics;
    }
    
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> memoryMetrics = new HashMap<>();
        
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // 堆内存
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
            
            memoryMetrics.put("heapUsed", heapUsed);
            memoryMetrics.put("heapMax", heapMax);
            memoryMetrics.put("heapCommitted", heapCommitted);
            memoryMetrics.put("heapUsagePercent", heapMax > 0 ? Math.round((double) heapUsed / heapMax * 10000) / 100.0 : 0);
            
            // 非堆内存
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            long nonHeapCommitted = memoryBean.getNonHeapMemoryUsage().getCommitted();
            
            memoryMetrics.put("nonHeapUsed", nonHeapUsed);
            memoryMetrics.put("nonHeapMax", nonHeapMax);
            memoryMetrics.put("nonHeapCommitted", nonHeapCommitted);
            memoryMetrics.put("nonHeapUsagePercent", nonHeapMax > 0 ? Math.round((double) nonHeapUsed / nonHeapMax * 10000) / 100.0 : 0);
            
        } catch (Exception e) {
            log.warn("Failed to get memory metrics", e);
        }
        
        return memoryMetrics;
    }
    
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        try {
            systemMetrics.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            systemMetrics.put("freeMemory", Runtime.getRuntime().freeMemory());
            systemMetrics.put("totalMemory", Runtime.getRuntime().totalMemory());
            systemMetrics.put("maxMemory", Runtime.getRuntime().maxMemory());
            
            // 计算内存使用百分比
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0;
            
            systemMetrics.put("memoryUsagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);
            
        } catch (Exception e) {
            log.warn("Failed to get system metrics", e);
        }
        
        return systemMetrics;
    }
    
    private Map<String, Object> getApplicationMetrics() {
        Map<String, Object> applicationMetrics = new HashMap<>();
        
        try {
            applicationMetrics.put("name", applicationName);
            applicationMetrics.put("version", serverVersion);
            applicationMetrics.put("startTime", ManagementFactory.getRuntimeMXBean().getStartTime());
            applicationMetrics.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
            applicationMetrics.put("javaVersion", System.getProperty("java.version"));
            applicationMetrics.put("osName", System.getProperty("os.name"));
            applicationMetrics.put("osVersion", System.getProperty("os.version"));
            
        } catch (Exception e) {
            log.warn("Failed to get application metrics", e);
        }
        
        return applicationMetrics;
    }
}
