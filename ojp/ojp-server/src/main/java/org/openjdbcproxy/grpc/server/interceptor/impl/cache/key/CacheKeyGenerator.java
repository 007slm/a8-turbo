package org.openjdbcproxy.grpc.server.interceptor.impl.cache.key;

import com.openjdbcproxy.grpc.SessionInfo;
import com.openjdbcproxy.grpc.StatementRequest;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.SerializationHandler;
import org.springframework.stereotype.Component;

/**
 * 缓存键生成器
 * 负责生成唯一的缓存键
 */
@Slf4j
@Component
public class CacheKeyGenerator {
    
    /**
     * 生成缓存键
     * 格式：ojp:cache:query:{connHash}:{sqlHash}:{paramsHash}
     */
    public String generateCacheKey(StatementRequest request, SessionInfo session) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("ojp:cache:query:");
        keyBuilder.append(session.getConnHash());
        keyBuilder.append(":");
        
        // 添加SQL的哈希值
        String sql = request.getSql().trim();
        keyBuilder.append(sql.hashCode());
        
        // 添加参数的哈希值
        if (!request.getParameters().isEmpty()) {
            try {
                String paramsStr = SerializationHandler.deserializeParams(request.getParameters());
                keyBuilder.append(":params:");
                keyBuilder.append(paramsStr.hashCode());
            } catch (Exception e) {
                log.warn("Failed to serialize parameters for cache key", e);
            }
        }
        
        return keyBuilder.toString();
    }
}
