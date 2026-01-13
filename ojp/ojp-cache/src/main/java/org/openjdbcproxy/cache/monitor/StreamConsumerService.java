package org.openjdbcproxy.cache.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.cache.monitor.metrics.CacheMetrics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class StreamConsumerService implements InitializingBean {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TableSyncStateManager tableSyncStateManager;
    private final CacheMetrics cacheMetrics;
    
    private static final String STREAM_KEY = "ojp:seatunnel:events";
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);

    public StreamConsumerService(StringRedisTemplate redisTemplate, 
                                  ObjectMapper objectMapper,
                                  TableSyncStateManager tableSyncStateManager,
                                  CacheMetrics cacheMetrics) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tableSyncStateManager = tableSyncStateManager;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    public void afterPropertiesSet() {
        executorService.submit(this::consumeStream);
    }
    
    private void consumeStream() {
        log.info("正在启动 Redis Stream 消费者: {}", STREAM_KEY);
        // Start from beginning to reconstruct state, since we don't persist offset yet and need latest state
        String lastId = "0-0";
        int retryCount = 0;
        
        while (running.get()) {
            try {
                StreamReadOptions readOptions = StreamReadOptions.empty().block(Duration.ofMillis(2000));
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                        .read(readOptions, StreamOffset.create(STREAM_KEY, ReadOffset.from(lastId)));
                
                if (records != null && !records.isEmpty()) {
                    for (MapRecord<String, Object, Object> record : records) {
                        lastId = record.getId().getValue();
                        processEvent(record.getValue());
                    }
                    retryCount = 0;
                }
            } catch (Exception e) {
                log.error("消费 Redis Stream 时发生错误", e);
                retryCount++;
                long backoffMs = Math.min(1000L * (1 << Math.min(retryCount, 6)), 30_000);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void processEvent(Map<Object, Object> body) {
        try {
            Map<String, String> event = new HashMap<>();
            for (Map.Entry<Object, Object> entry : body.entrySet()) {
                event.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            
            tableSyncStateManager.updateFromStreamEvent(event);
            cacheMetrics.recordStreamEvent();
            
            log.debug("已处理流事件: type={}, table={}", 
                    event.get("type"), event.get("table"));
            
        } catch (Exception e) {
            log.error("处理事件失败: {}", body, e);
        }
    }
    
    @PreDestroy
    public void stop() {
        running.set(false);
        executorService.shutdown();
    }
}
