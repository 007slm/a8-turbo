package org.openjdbcproxy.grpc.server.config;

import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.OjpHealthManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 健康管理器初始化组件
 * 在应用启动完成后初始化 OjpHealthManager
 */
@Slf4j
@Component
public class HealthManagerInitializer {
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeHealthManager() {
        log.info("Initializing OjpHealthManager...");
        OjpHealthManager.initialize();
        log.info("OjpHealthManager initialized successfully");
    }
}
