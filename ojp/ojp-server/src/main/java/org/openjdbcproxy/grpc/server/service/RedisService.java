package org.openjdbcproxy.grpc.server.service;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.config.RedisConfig;
import org.openjdbcproxy.grpc.server.dto.RedisConnectionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务类
 * 提供统一的Redis操作接口，参考smart-redis-cache实现
 */
@Slf4j
@Service
public class RedisService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedisConfig redisConfig;

    /**
     * 测试Redis连接
     */
    public boolean testConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis连接测试失败", e);
            return false;
        }
    }

    /**
     * Redis ping
     */
    public String ping() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG";
        } catch (Exception e) {
            log.error("Redis ping失败", e);
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 获取Redis连接状态
     */
    public RedisConnectionStatus getConnectionStatus() {
        try {
            String pingResponse = ping();
            return RedisConnectionStatus.builder()
                    .connected(true)
                    .ping(pingResponse)
                    .timestamp(System.currentTimeMillis())
                    .details(getConnectionDetails())
                    .build();
        } catch (Exception e) {
            return RedisConnectionStatus.builder()
                    .connected(false)
                    .error(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }
    
    /**
     * 获取连接详情
     */
    private RedisConnectionStatus.ConnectionDetails getConnectionDetails() {
        return RedisConnectionStatus.ConnectionDetails.builder()
                .host(redisConfig.getHost())
                .port(redisConfig.getPort())
                .database(redisConfig.getDatabase())
                .poolSize(redisConfig.getMaxActive())
                .activeConnections(redisConfig.getMaxActive() - redisConfig.getMaxIdle())
                .idleConnections(redisConfig.getMaxIdle())
                .connectionTimeout(redisConfig.getTimeout().longValue())
                .readTimeout(redisConfig.getTimeout().longValue())
                .build();
    }

    // ====================== 基础操作 ======================

    /**
     * 设置键值对
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置键值对并设置过期时间
     */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 设置键值对并设置过期时间（毫秒）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取值并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除键
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 检查键是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    /**
     * 设置过期时间（毫秒）
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     */
    public Duration getExpire(String key) {
        Long seconds = redisTemplate.getExpire(key);
        return seconds != null ? Duration.ofSeconds(seconds) : null;
    }

    /**
     * 获取过期时间（毫秒）
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    // ====================== Hash操作 ======================

    /**
     * 设置Hash字段
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取Hash字段
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取Hash所有字段
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除Hash字段
     */
    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 检查Hash字段是否存在
     */
    public Boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 获取Hash字段数量
     */
    public Long hSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    // ====================== List操作 ======================

    /**
     * 从左侧推入列表
     */
    public Long lPush(String key, Object... values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 从右侧推入列表
     */
    public Long rPush(String key, Object... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 从左侧弹出列表
     */
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 从右侧弹出列表
     */
    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表范围
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ====================== Set操作 ======================

    /**
     * 添加Set元素
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 移除Set元素
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 获取Set所有元素
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 检查Set是否包含元素
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取Set大小
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // ====================== ZSet操作 ======================

    /**
     * 添加ZSet元素
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取ZSet元素分数
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 获取ZSet范围
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取ZSet范围（带分数）
     */
    public Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
    }

    /**
     * 获取ZSet大小
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    // ====================== 数据库操作 ======================

    /**
     * 选择数据库
     */
    public void select(int dbIndex) {
        redisTemplate.getConnectionFactory().getConnection().select(dbIndex);
    }

    /**
     * 清空当前数据库
     */
    public void flushDb() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    /**
     * 清空所有数据库
     */
    public void flushAll() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    /**
     * 获取数据库大小
     */
    public Long dbSize() {
        return redisTemplate.getConnectionFactory().getConnection().dbSize();
    }

    // ====================== 键操作 ======================

    /**
     * 获取所有键
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 获取键类型
     */
    public String type(String key) {
        return redisTemplate.type(key).name();
    }

    /**
     * 重命名键
     */
    public void rename(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    // ====================== 原子操作 ======================

    /**
     * 原子递增
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 原子递增指定值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 原子递减
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 原子递减指定值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ====================== 脚本执行 ======================

    /**
     * 执行Lua脚本
     */
    public <T> T executeScript(DefaultRedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    // ====================== 事务操作 ======================

    /**
     * 执行事务
     */
    @SuppressWarnings("unchecked")
    public List<Object> executeTransaction(org.springframework.data.redis.core.SessionCallback<?> session) {
        return (List<Object>) redisTemplate.execute(session);
    }

    // ====================== 管道操作 ======================

    /**
     * 执行管道操作
     */
    public List<Object> executePipelined(org.springframework.data.redis.core.SessionCallback<?> session) {
        return redisTemplate.executePipelined(session);
    }
}
