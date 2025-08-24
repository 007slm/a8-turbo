package io.nats.demo.status;

import com.alibaba.fastjson.JSONObject;
import io.nats.client.*;
import io.nats.client.impl.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 监控服务示例
 * 监听配对的subject以获取原始subject的实时状态
 */
@Configuration
@Slf4j
public class TableCdcSyncStatusMonitorConfiguration {
    ConcurrentHashMap<String, TableStatus> tableStatusMap = new ConcurrentHashMap<>();

    /**
     * msg
     * 1.  cdc msg 有数据变化
     * 2.  flink msg 数据变化 已经开始同步
     * 3.  flink msg 数据变化 已经同步完毕
     *
     */
    @Bean
    public Consumer<Message> onCdcMessageChange() {
        return msg -> {
            Headers headers = msg.getHeaders();
//            String eventType = headers.getFirst("eventType");
//            String operationType = headers.getFirst("operationType");
//            Instant timestamp = Instant.ofEpochMilli(Long.valueOf(headers.getFirst("timestamp")));
           log.info("cdc msg %s", JSONObject.toJSONString(msg));
        };
    }
}