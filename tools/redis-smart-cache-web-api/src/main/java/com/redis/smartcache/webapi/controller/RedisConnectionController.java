package com.redis.smartcache.webapi.controller;

import com.redis.smartcache.webapi.model.ApiResponse;
import com.redis.smartcache.webapi.service.RedisSmartCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Redis连接管理控制器
 * 提供Redis连接测试、状态查询等功能
 */
@RestController
@RequestMapping("/api/redis")
@Tag(name = "Redis连接管理", description = "Redis连接相关的API接口")
@CrossOrigin(origins = "*")
public class RedisConnectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionController.class);
    
    @Autowired
    private RedisSmartCacheService redisService;

    /**
     * Ping Redis服务器
     */
    @GetMapping("/ping")
    @Operation(summary = "Ping Redis", description = "测试Redis服务器响应")
    public ResponseEntity<ApiResponse<String>> ping() {
        try {
            String response = redisService.ping();
            return ResponseEntity.ok(ApiResponse.success("Redis ping成功", response));
        } catch (Exception e) {
            logger.error("Redis ping失败", e);
            return ResponseEntity.ok(ApiResponse.error("Redis ping失败: " + e.getMessage()));
        }
    }

    /**
     * 获取Redis连接状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取连接状态", description = "获取Redis连接状态和相关信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionStatus() {
        try {
            Map<String, Object> status = redisService.getConnectionStatus();
            return ResponseEntity.ok(ApiResponse.success("获取连接状态成功", status));
        } catch (Exception e) {
            logger.error("获取连接状态失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取连接状态失败: " + e.getMessage()));
        }
    }

    /**
     * 测试Redis连接
     */
    @PostMapping("/test-connection")
    @Operation(summary = "测试连接", description = "测试Redis连接是否正常")
    public ResponseEntity<ApiResponse<Boolean>> testConnection() {
        try {
            boolean connected = redisService.testConnection();
            if (connected) {
                return ResponseEntity.ok(ApiResponse.success("连接测试成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("连接测试失败"));
            }
        } catch (Exception e) {
            logger.error("连接测试失败", e);
            return ResponseEntity.ok(ApiResponse.error("连接测试失败: " + e.getMessage()));
        }
    }

    /**
     * 检查Smart Cache索引
     */
    @GetMapping("/check-index")
    @Operation(summary = "检查索引", description = "检查Smart Cache索引是否存在")
    public ResponseEntity<ApiResponse<Boolean>> checkIndex() {
        try {
            boolean exists = redisService.checkSmartCacheIndex();
            String message = exists ? "Smart Cache索引存在" : "Smart Cache索引不存在";
            return ResponseEntity.ok(ApiResponse.success(message, exists));
        } catch (Exception e) {
            logger.error("检查索引失败", e);
            return ResponseEntity.ok(ApiResponse.error("检查索引失败: " + e.getMessage()));
        }
    }

    /**
     * 获取应用名称
     */
    @GetMapping("/application-name")
    @Operation(summary = "获取应用名称", description = "获取当前Smart Cache应用名称")
    public ResponseEntity<ApiResponse<String>> getApplicationName() {
        try {
            String appName = redisService.getApplicationName();
            return ResponseEntity.ok(ApiResponse.success("获取应用名称成功", appName));
        } catch (Exception e) {
            logger.error("获取应用名称失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取应用名称失败: " + e.getMessage()));
        }
    }
}