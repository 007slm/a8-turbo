package org.openjdbcproxy.grpc.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.dto.ApiResponse;
import org.openjdbcproxy.grpc.server.service.CacheManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存列表、缓存详情、缓存键值管理等REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/caches")
@CrossOrigin(origins = "*")
public class CacheManagementController {

    @Autowired
    private CacheManagementService cacheManagementService;

    /**
     * 获取所有缓存列表
     * GET /api/caches
     */
    @GetMapping
    public ApiResponse<List<String>> getCaches() {
        try {
            List<String> caches = cacheManagementService.getCacheNames();
            return ApiResponse.success(caches, "获取缓存列表成功");
        } catch (Exception e) {
            log.error("Failed to get caches", e);
            return ApiResponse.error("GET_CACHES_ERROR", "获取缓存列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存详情
     * GET /api/caches/{name}
     */
    @GetMapping("/{name}")
    public ApiResponse<Map<String, Object>> getCache(@PathVariable String name) {
        try {
            Map<String, Object> cacheInfo = cacheManagementService.getCacheInfo(name);
            return ApiResponse.success(cacheInfo, "获取缓存详情成功");
        } catch (Exception e) {
            log.error("Failed to get cache: {}", name, e);
            return ApiResponse.error("GET_CACHE_ERROR", "获取缓存详情失败: " + e.getMessage());
        }
    }

    /**
     * 清空缓存
     * POST /api/caches/{name}/clear
     */
    @PostMapping("/{name}/clear")
    public ApiResponse<String> clearCache(@PathVariable String name) {
        try {
            cacheManagementService.clearCache(name);
            return ApiResponse.success("Cache cleared successfully", "清空缓存成功");
        } catch (Exception e) {
            log.error("Failed to clear cache: {}", name, e);
            return ApiResponse.error("CLEAR_CACHE_ERROR", "清空缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     * GET /api/caches/{name}/stats
     */
    @GetMapping("/{name}/stats")
    public ApiResponse<Map<String, Object>> getCacheStats(@PathVariable String name) {
        try {
            Map<String, Object> stats = cacheManagementService.getCacheStats(name);
            return ApiResponse.success(stats, "获取缓存统计成功");
        } catch (Exception e) {
            log.error("Failed to get cache stats: {}", name, e);
            return ApiResponse.error("GET_CACHE_STATS_ERROR", "获取缓存统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存键列表
     * GET /api/caches/{name}/keys
     */
    @GetMapping("/{name}/keys")
    public ApiResponse<List<String>> getCacheKeys(
            @PathVariable String name,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        try {
            List<String> keys = cacheManagementService.getCacheKeys(name, pattern, limit);
            return ApiResponse.success(keys, "获取缓存键列表成功");
        } catch (Exception e) {
            log.error("Failed to get cache keys: {}", name, e);
            return ApiResponse.error("GET_CACHE_KEYS_ERROR", "获取缓存键列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存值
     * GET /api/caches/{name}/keys/{key}
     */
    @GetMapping("/{name}/keys/{key}")
    public ApiResponse<Object> getCacheValue(@PathVariable String name, @PathVariable String key) {
        try {
            Object value = cacheManagementService.getCacheValue(name, key);
            return ApiResponse.success(value, "获取缓存值成功");
        } catch (Exception e) {
            log.error("Failed to get cache value: {}:{}", name, key, e);
            return ApiResponse.error("GET_CACHE_VALUE_ERROR", "获取缓存值失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存键
     * DELETE /api/caches/{name}/keys/{key}
     */
    @DeleteMapping("/{name}/keys/{key}")
    public ApiResponse<String> deleteCacheKey(@PathVariable String name, @PathVariable String key) {
        try {
            cacheManagementService.deleteCacheKey(name, key);
            return ApiResponse.success("Cache key deleted successfully", "删除缓存键成功");
        } catch (Exception e) {
            log.error("Failed to delete cache key: {}:{}", name, key, e);
            return ApiResponse.error("DELETE_CACHE_KEY_ERROR", "删除缓存键失败: " + e.getMessage());
        }
    }

    /**
     * 设置缓存值
     * PUT /api/caches/{name}/keys/{key}
     */
    @PutMapping("/{name}/keys/{key}")
    public ApiResponse<String> setCacheValue(
            @PathVariable String name, 
            @PathVariable String key, 
            @RequestBody Object value) {
        try {
            cacheManagementService.setCacheValue(name, key, value);
            return ApiResponse.success("Cache value set successfully", "设置缓存值成功");
        } catch (Exception e) {
            log.error("Failed to set cache value: {}:{}", name, key, e);
            return ApiResponse.error("SET_CACHE_VALUE_ERROR", "设置缓存值失败: " + e.getMessage());
        }
    }
}
