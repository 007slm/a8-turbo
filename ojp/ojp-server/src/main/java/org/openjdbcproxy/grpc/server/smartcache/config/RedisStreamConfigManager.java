package org.openjdbcproxy.grpc.server.smartcache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.XReadArgs.StreamOffset;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.config.RedisConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Redis Stream配置管理器
 * 负责监听Redis Stream中的配置变更并通知更新
 * Redis是核心依赖，负责缓存规则配置的存储和同步
 */
@Slf4j
@Component
public class RedisStreamConfigManager {
    
    private static final String CONFIG_STREAM_KEY = "ojp:smartcache:config:stream";
    private static final String CONFIG_KEY = "ojp:smartcache:config";
    private static final Duration POLL_BLOCK_DURATION = Duration.ofMillis(1000);
    
    @Autowired
    private RedisConnectionManager redisConnectionManager;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService streamPoller = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private Consumer<CacheRuleConfig> configUpdateListener;
    
    @PostConstruct
    public void init() {
        try {
            // 初始化配置流
            initializeConfigStream();
            
            // 启动配置监听
            startConfigListener();
            
            log.info("Redis Stream配置管理器初始化成功");
        } catch (Exception e) {
            log.error("Redis Stream配置管理器初始化失败", e);
            throw new RuntimeException("Redis Stream配置管理器初始化失败", e);
        }
    }
    
    /**
     * 设置配置更新监听器
     */
    public void setConfigUpdateListener(Consumer<CacheRuleConfig> listener) {
        this.configUpdateListener = listener;
    }
    
    /**
     * 保存配置到Redis Stream
     */
    public void saveConfig(CacheRuleConfig config) {
        try {
            String configJson = objectMapper.writeValueAsString(config);
            
            // 保存到配置键
            redisConnectionManager.getCommands().set(CONFIG_KEY, configJson);
            
            // 添加到Stream
            redisConnectionManager.getCommands().xadd(CONFIG_STREAM_KEY, "config", configJson);
            
            log.info("配置已保存到Redis: {}", config.getName());
        } catch (Exception e) {
            log.error("保存配置到Redis失败", e);
            throw new RuntimeException("保存配置失败", e);
        }
    }
    
    /**
     * 从Redis加载配置
     */
    public CacheRuleConfig loadConfig() {
        try {
            String configJson = redisConnectionManager.getCommands().get(CONFIG_KEY);
            if (configJson != null) {
                return objectMapper.readValue(configJson, CacheRuleConfig.class);
            }
        } catch (Exception e) {
            log.error("从Redis加载配置失败", e);
        }
        return null;
    }
    
    /**
     * 初始化配置流
     */
    private void initializeConfigStream() {
        try {
            // 检查Stream是否存在，如果不存在则创建
            Long streamLength = redisConnectionManager.getCommands().xlen(CONFIG_STREAM_KEY);
            if (streamLength == 0) {
                // 创建初始配置
                CacheRuleConfig initialConfig = createDefaultConfig();
                saveConfig(initialConfig);
                log.info("创建了默认配置");
            }
        } catch (Exception e) {
            log.error("初始化配置流失败", e);
        }
    }
    
    /**
     * 启动配置监听器
     */
    private void startConfigListener() {
        running.set(true);
        streamPoller.submit(() -> {
            try {
                while (running.get()) {
                    pollConfigUpdates();
                    Thread.sleep(100); // 短暂休眠避免CPU占用过高
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("配置监听器被中断");
            } catch (Exception e) {
                log.error("配置监听器运行异常", e);
            }
        });
    }
    
    /**
     * 轮询配置更新
     */
    private void pollConfigUpdates() {
        try {
            // 从最新位置读取Stream消息
            List<StreamMessage<String, String>> messages = redisConnectionManager.getCommands()
                    .xread(XReadArgs.Builder.block(POLL_BLOCK_DURATION), 
                           StreamOffset.latest(CONFIG_STREAM_KEY));
            
            for (StreamMessage<String, String> message : messages) {
                processConfigMessage(message);
            }
        } catch (Exception e) {
            log.warn("轮询配置更新失败", e);
        }
    }
    
    /**
     * 处理配置消息
     */
    private void processConfigMessage(StreamMessage<String, String> message) {
        try {
            String configJson = message.getBody().get("config");
            if (configJson != null) {
                CacheRuleConfig config = objectMapper.readValue(configJson, CacheRuleConfig.class);
                
                log.info("收到配置更新: {}", config.getName());
                
                // 通知监听器
                if (configUpdateListener != null) {
                    configUpdateListener.accept(config);
                }
            }
        } catch (Exception e) {
            log.error("处理配置消息失败", e);
        }
    }
    
    /**
     * 创建默认配置
     */
    private CacheRuleConfig createDefaultConfig() {
        CacheRuleConfig config = new CacheRuleConfig();
        config.setName("default");
        config.setDescription("默认缓存规则配置");
        config.setEnabled(true);
        return config;
    }
    
    @PreDestroy
    public void destroy() {
        running.set(false);
        if (streamPoller != null) {
            streamPoller.shutdown();
        }
        log.info("Redis Stream配置管理器已关闭");
    }
}
