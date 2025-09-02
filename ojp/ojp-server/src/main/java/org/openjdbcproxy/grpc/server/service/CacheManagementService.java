package org.openjdbcproxy.grpc.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理服务
 * 提供缓存列表、缓存详情、缓存键值管理等功能
 */
@Service
public class CacheManagementService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取所有缓存名称
     */
    public List<String> getCacheNames() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys == null) {
                return new ArrayList<>();
            }
            
            // 过滤出缓存名称（去掉具体的键名）
            Set<String> cacheNames = new HashSet<>();
            for (String key : keys) {
                String[] parts = key.split(":");
                if (parts.length > 0) {
                    cacheNames.add(parts[0]);
                }
            }
            
            return new ArrayList<>(cacheNames);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cache names", e);
        }
    }

    /**
     * 获取缓存详情
     */
    public Map<String, Object> getCacheInfo(String name) {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("name", name);
            
            // 获取缓存键数量
            Set<String> keys = redisTemplate.keys(name + ":*");
            info.put("keyCount", keys != null ? keys.size() : 0);
            
            // 获取缓存大小（估算）
            long totalSize = 0;
            if (keys != null) {
                for (String key : keys) {
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        totalSize += value.toString().length();
                    }
                }
            }
            info.put("totalSize", totalSize);
            
            return info;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cache info: " + name, e);
        }
    }

    /**
     * 清空缓存
     */
    public void clearCache(String name) {
        try {
            Set<String> keys = redisTemplate.keys(name + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear cache: " + name, e);
        }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats(String name) {
        try {
            Map<String, Object> stats = new HashMap<>();
            Set<String> keys = redisTemplate.keys(name + ":*");
            
            if (keys == null) {
                stats.put("keyCount", 0);
                stats.put("totalSize", 0);
                stats.put("avgSize", 0);
                return stats;
            }
            
            stats.put("keyCount", keys.size());
            
            long totalSize = 0;
            int validKeys = 0;
            
            for (String key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    totalSize += value.toString().length();
                    validKeys++;
                }
            }
            
            stats.put("totalSize", totalSize);
            stats.put("avgSize", validKeys > 0 ? totalSize / validKeys : 0);
            
            return stats;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cache stats: " + name, e);
        }
    }

    /**
     * 获取缓存键列表
     */
    public List<String> getCacheKeys(String name, String pattern, int limit) {
        try {
            String searchPattern = name + ":" + (pattern != null ? pattern : "*");
            Set<String> keys = redisTemplate.keys(searchPattern);
            
            if (keys == null) {
                return new ArrayList<>();
            }
            
            List<String> keyList = new ArrayList<>(keys);
            Collections.sort(keyList);
            
            // 限制返回数量
            if (keyList.size() > limit) {
                keyList = keyList.subList(0, limit);
            }
            
            return keyList;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cache keys: " + name, e);
        }
    }

    /**
     * 获取缓存值
     */
    public Object getCacheValue(String name, String key) {
        try {
            String fullKey = name + ":" + key;
            return redisTemplate.opsForValue().get(fullKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get cache value: " + name + ":" + key, e);
        }
    }

    /**
     * 删除缓存键
     */
    public void deleteCacheKey(String name, String key) {
        try {
            String fullKey = name + ":" + key;
            redisTemplate.delete(fullKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete cache key: " + name + ":" + key, e);
        }
    }

    /**
     * 设置缓存值
     */
    public void setCacheValue(String name, String key, Object value) {
        try {
            String fullKey = name + ":" + key;
            redisTemplate.opsForValue().set(fullKey, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set cache value: " + name + ":" + key, e);
        }
    }

    /**
     * 设置缓存值（带过期时间）
     */
    public void setCacheValue(String name, String key, Object value, long timeout, TimeUnit unit) {
        try {
            String fullKey = name + ":" + key;
            redisTemplate.opsForValue().set(fullKey, value, timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set cache value: " + name + ":" + key, e);
        }
    }
}
