package org.openjdbcproxy.grpc.server.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;

/**
 * Redis连接管理器
 * Redis是核心依赖，负责缓存规则、SQL统计和表信息统计的存储
 */
@Slf4j
@Component
public class RedisConnectionManager {
    
    @Autowired
    private RedisConfig redisConfig;
    
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    
    @PostConstruct
    public void init() {
        try {
            log.info("正在初始化Redis连接...");
            log.info("Redis配置: host={}, port={}, database={}, timeout={}ms", 
                    redisConfig.getHost(), redisConfig.getPort(), 
                    redisConfig.getDatabase(), redisConfig.getTimeout());
            
            // 构建Redis URI
            RedisURI.Builder uriBuilder = RedisURI.builder()
                    .withHost(redisConfig.getHost())
                    .withPort(redisConfig.getPort())
                    .withDatabase(redisConfig.getDatabase())
                    .withTimeout(Duration.ofMillis(redisConfig.getTimeout()));
            
            if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
                uriBuilder.withPassword(redisConfig.getPassword());
            }
            
            if (redisConfig.isSsl()) {
                uriBuilder.withSsl(true);
            }
            
            RedisURI redisURI = uriBuilder.build();
            log.info("Redis URI: {}", redisURI.toString());

            // 创建Redis客户端
            redisClient = RedisClient.create(redisURI);
            
            // 创建连接
            connection = redisClient.connect();
            
            // 测试连接
            RedisCommands<String, String> commands = connection.sync();
            String pong = commands.ping();
            log.info("Redis连接成功: {}", pong);
            
        } catch (Exception e) {
            log.error("Redis连接失败，但应用程序将继续启动。请检查Redis服务是否运行。", e);
            log.error("Redis连接失败详情: {}", e.getMessage());
            log.error("请确保Redis服务在 {}:{} 上运行", redisConfig.getHost(), redisConfig.getPort());
            log.error("应用程序将在Redis连接恢复后自动重连");
            
            // 不抛出异常，让应用程序继续启动
            // 后续操作会通过 isConnected() 检查连接状态
        }
    }
    
    /**
     * 获取Redis命令接口
     */
    public RedisCommands<String, String> getCommands() {
        if (!isConnected()) {
            throw new IllegalStateException("Redis连接未建立或已关闭，请检查Redis服务是否运行");
        }
        return connection.sync();
    }
    
    /**
     * 获取Redis客户端
     */
    public RedisClient getRedisClient() {
        return redisClient;
    }
    
    /**
     * 获取Redis连接
     */
    public StatefulRedisConnection<String, String> getConnection() {
        return connection;
    }
    
    /**
     * 检查连接是否可用
     */
    public boolean isConnected() {
        try {
            if (connection != null && connection.isOpen()) {
                connection.sync().ping();
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis连接检查失败: {}", e.getMessage());
            // 尝试重新连接
            attemptReconnect();
        }
        return false;
    }
    
    /**
     * 尝试重新连接Redis
     */
    private void attemptReconnect() {
        try {
            log.info("尝试重新连接Redis...");
            
            // 关闭旧连接
            if (connection != null) {
                connection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
            
            // 重新初始化
            init();
            
        } catch (Exception e) {
            log.warn("Redis重连失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取Redis连接状态信息
     */
    public RedisStatus getStatus() {
        return new RedisStatus(
            isConnected(),
            redisConfig.getHost(),
            redisConfig.getPort(),
            connection != null && connection.isOpen()
        );
    }
    
    /**
     * Redis状态信息
     */
    public static class RedisStatus {
        private final boolean connected;
        private final String host;
        private final int port;
        private final boolean connectionOpen;
        
        public RedisStatus(boolean connected, String host, int port, boolean connectionOpen) {
            this.connected = connected;
            this.host = host;
            this.port = port;
            this.connectionOpen = connectionOpen;
        }
        
        public boolean isConnected() { return connected; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public boolean isConnectionOpen() { return connectionOpen; }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (redisClient != null) {
                redisClient.shutdown();
            }
            log.info("Redis连接已关闭");
        } catch (Exception e) {
            log.error("关闭Redis连接时发生错误", e);
        }
    }
}
