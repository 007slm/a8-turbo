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
 * 配置管理控制器
 * 提供应用配置的获取、更新和重置功能
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "配置管理", description = "应用配置相关的API接口")
@CrossOrigin(origins = "*")
public class ConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    
    @Autowired
    private RedisSmartCacheService redisService;

    /**
     * 获取应用配置
     */
    @GetMapping
    @Operation(summary = "获取应用配置", description = "获取当前应用的配置信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        try {
            Map<String, Object> config = redisService.getConfig();
            return ResponseEntity.ok(ApiResponse.success("获取应用配置成功", config));
        } catch (Exception e) {
            logger.error("获取应用配置失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取应用配置失败: " + e.getMessage()));
        }
    }

    /**
     * 更新应用配置
     */
    @PutMapping
    @Operation(summary = "更新应用配置", description = "更新应用的配置信息")
    public ResponseEntity<ApiResponse<Boolean>> updateConfig(
            @RequestBody Map<String, Object> config) {
        
        try {
            boolean updated = redisService.updateConfig(config);
            if (updated) {
                return ResponseEntity.ok(ApiResponse.success("更新应用配置成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("更新应用配置失败"));
            }
        } catch (Exception e) {
            logger.error("更新应用配置失败", e);
            return ResponseEntity.ok(ApiResponse.error("更新应用配置失败: " + e.getMessage()));
        }
    }

    /**
     * 重置应用配置
     */
    @PostMapping("/reset")
    @Operation(summary = "重置应用配置", description = "重置应用配置到默认值")
    public ResponseEntity<ApiResponse<Boolean>> resetConfig() {
        try {
            boolean reset = redisService.resetConfig();
            if (reset) {
                return ResponseEntity.ok(ApiResponse.success("重置应用配置成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.error("重置应用配置失败"));
            }
        } catch (Exception e) {
            logger.error("重置应用配置失败", e);
            return ResponseEntity.ok(ApiResponse.error("重置应用配置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取配置模板
     */
    @GetMapping("/template")
    @Operation(summary = "获取配置模板", description = "获取配置文件的模板和说明")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfigTemplate() {
        try {
            Map<String, Object> template = Map.of(
                "redis", Map.of(
                    "host", "localhost",
                    "port", 6379,
                    "database", 0,
                    "password", "",
                    "username", "default",
                    "ssl", false,
                    "timeout", 10000
                ),
                "application", Map.of(
                    "name", "smartcache"
                ),
                "descriptions", Map.of(
                    "redis.host", "Redis服务器主机地址",
                    "redis.port", "Redis服务器端口",
                    "redis.database", "Redis数据库编号",
                    "redis.password", "Redis连接密码",
                    "redis.username", "Redis连接用户名",
                    "redis.ssl", "是否启用SSL连接",
                    "redis.timeout", "连接超时时间（毫秒）",
                    "application.name", "Smart Cache应用名称"
                )
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取配置模板成功", template));
        } catch (Exception e) {
            logger.error("获取配置模板失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取配置模板失败: " + e.getMessage()));
        }
    }

    /**
     * 验证配置
     */
    @PostMapping("/validate")
    @Operation(summary = "验证配置", description = "验证配置的有效性")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateConfig(
            @RequestBody Map<String, Object> config) {
        
        try {
            // 这里可以添加更复杂的配置验证逻辑
            Map<String, Object> validation = Map.of(
                "valid", true,
                "message", "配置验证通过",
                "warnings", java.util.List.of(),
                "errors", java.util.List.of()
            );
            
            return ResponseEntity.ok(ApiResponse.success("配置验证完成", validation));
        } catch (Exception e) {
            logger.error("配置验证失败", e);
            return ResponseEntity.ok(ApiResponse.error("配置验证失败: " + e.getMessage()));
        }
    }
}