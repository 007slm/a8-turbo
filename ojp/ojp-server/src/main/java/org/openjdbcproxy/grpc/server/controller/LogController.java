package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志管理控制器
 * 提供日志相关的API端点
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    
    private static final String LOG_DIR = "logs";
    private static final String APPLICATION_LOG = "application.log";
    private static final String ACCESS_LOG = "access.log";
    private static final String ERROR_LOG = "error.log";
    
    /**
     * 5.1 应用日志
     * GET /api/logs/application
     */
    @GetMapping("/application")
    public ApiResponse<Object> getApplicationLogs(
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search) {
        try {
            log.debug("Getting application logs: lines={}, offset={}, level={}, search={}", 
                     lines, offset, level, search);
            
            List<String> logLines = readLogFile(APPLICATION_LOG, lines, offset);
            if (level != null || search != null) {
                logLines = filterLogs(logLines, level, search);
            }
            
            return ApiResponse.success(logLines, "获取应用日志成功");
        } catch (Exception e) {
            log.error("Failed to get application logs", e);
            return ApiResponse.error("APPLICATION_LOG_ERROR", "获取应用日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 5.2 访问日志
     * GET /api/logs/access
     */
    @GetMapping("/access")
    public ApiResponse<Object> getAccessLogs(
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String path) {
        try {
            log.debug("Getting access logs: lines={}, offset={}, ip={}, path={}", 
                     lines, offset, ip, path);
            
            List<String> logLines = readLogFile(ACCESS_LOG, lines, offset);
            if (ip != null || path != null) {
                logLines = filterAccessLogs(logLines, ip, path);
            }
            
            return ApiResponse.success(logLines, "获取访问日志成功");
        } catch (Exception e) {
            log.error("Failed to get access logs", e);
            return ApiResponse.error("ACCESS_LOG_ERROR", "获取访问日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 5.3 错误日志
     * GET /api/logs/error
     */
    @GetMapping("/error")
    public ApiResponse<Object> getErrorLogs(
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String errorType,
            @RequestParam(required = false) String search) {
        try {
            log.debug("Getting error logs: lines={}, offset={}, errorType={}, search={}", 
                     lines, offset, errorType, search);
            
            List<String> logLines = readLogFile(ERROR_LOG, lines, offset);
            if (errorType != null || search != null) {
                logLines = filterErrorLogs(logLines, errorType, search);
            }
            
            return ApiResponse.success(logLines, "获取错误日志成功");
        } catch (Exception e) {
            log.error("Failed to get error logs", e);
            return ApiResponse.error("ERROR_LOG_ERROR", "获取错误日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取日志文件列表
     * GET /api/logs/files
     */
    @GetMapping("/files")
    public ApiResponse<Object> getLogFiles() {
        try {
            log.debug("Getting log files list");
            
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                // 如果日志目录不存在，创建它并返回默认日志文件
                Files.createDirectories(logPath);
                createDefaultLogFiles();
            }
            
            List<File> logFiles = Files.list(logPath)
                    .filter(path -> path.toString().endsWith(".log"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            
            List<Object> fileInfo = logFiles.stream()
                    .map(file -> {
                        try {
                            return java.util.Map.of(
                                "name", file.getName(),
                                "size", file.length(),
                                "lastModified", file.lastModified(),
                                "readable", file.canRead()
                            );
                        } catch (Exception e) {
                            return java.util.Map.of("name", file.getName(), "error", e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());
            
            return ApiResponse.success(fileInfo, "获取日志文件列表成功");
        } catch (Exception e) {
            log.error("Failed to get log files list", e);
            return ApiResponse.error("LOG_FILES_ERROR", "获取日志文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理日志文件
     * DELETE /api/logs/cleanup
     */
    @DeleteMapping("/cleanup")
    public ApiResponse<String> cleanupLogs(@RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            log.debug("Cleaning up logs older than {} days", daysToKeep);
            
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                return ApiResponse.success("No logs to clean", "没有需要清理的日志");
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
            int cleanedCount = 0;
            
            List<Path> logFiles = Files.list(logPath)
                    .filter(path -> path.toString().endsWith(".log"))
                    .collect(Collectors.toList());
            
            for (Path logFile : logFiles) {
                try {
                    if (Files.getLastModifiedTime(logFile).toMillis() < cutoffTime) {
                        Files.delete(logFile);
                        cleanedCount++;
                    }
                } catch (IOException e) {
                    log.warn("Failed to delete old log file: {}", logFile, e);
                }
            }
            
            return ApiResponse.success("Cleaned " + cleanedCount + " log files", 
                                     "清理了 " + cleanedCount + " 个日志文件");
        } catch (Exception e) {
            log.error("Failed to cleanup logs", e);
            return ApiResponse.error("LOG_CLEANUP_ERROR", "清理日志失败: " + e.getMessage());
        }
    }
    
    // 私有辅助方法
    
    private List<String> readLogFile(String fileName, int lines, int offset) throws IOException {
        Path logPath = Paths.get(LOG_DIR, fileName);
        if (!Files.exists(logPath)) {
            createDefaultLogFile(fileName);
        }
        
        List<String> allLines = Files.readAllLines(logPath);
        int startIndex = Math.max(0, allLines.size() - lines - offset);
        int endIndex = Math.max(0, allLines.size() - offset);
        
        if (startIndex >= endIndex) {
            return new ArrayList<>();
        }
        
        return allLines.subList(startIndex, endIndex);
    }
    
    private List<String> filterLogs(List<String> logLines, String level, String search) {
        return logLines.stream()
                .filter(line -> {
                    if (level != null && !line.toLowerCase().contains(level.toLowerCase())) {
                        return false;
                    }
                    if (search != null && !line.toLowerCase().contains(search.toLowerCase())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private List<String> filterAccessLogs(List<String> logLines, String ip, String path) {
        return logLines.stream()
                .filter(line -> {
                    if (ip != null && !line.contains(ip)) {
                        return false;
                    }
                    if (path != null && !line.contains(path)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private List<String> filterErrorLogs(List<String> logLines, String errorType, String search) {
        return logLines.stream()
                .filter(line -> {
                    if (errorType != null && !line.toLowerCase().contains(errorType.toLowerCase())) {
                        return false;
                    }
                    if (search != null && !line.toLowerCase().contains(search.toLowerCase())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private void createDefaultLogFiles() throws IOException {
        createDefaultLogFile(APPLICATION_LOG);
        createDefaultLogFile(ACCESS_LOG);
        createDefaultLogFile(ERROR_LOG);
    }
    
    private void createDefaultLogFile(String fileName) throws IOException {
        Path logPath = Paths.get(LOG_DIR, fileName);
        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath.getParent());
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String defaultContent = String.format("[%s] INFO - Default log file created for %s%n", 
                                               timestamp, fileName);
            
            Files.write(logPath, defaultContent.getBytes());
        }
    }
}
