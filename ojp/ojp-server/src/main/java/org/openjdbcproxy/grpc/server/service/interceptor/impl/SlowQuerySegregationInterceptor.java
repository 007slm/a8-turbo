package org.openjdbcproxy.grpc.server.service.interceptor.impl;

import com.openjdbcproxy.grpc.ConnectionDetails;
import com.openjdbcproxy.grpc.SessionInfo;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.ServerConfiguration;
import org.openjdbcproxy.grpc.server.SlowQuerySegregationManager;
import org.openjdbcproxy.grpc.server.service.interceptor.context.CurrentRequestContext;
import org.openjdbcproxy.grpc.server.service.interceptor.StatementServiceInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断器拦截器，实现与业务代码中相同的熔断器逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlowQuerySegregationInterceptor implements StatementServiceInterceptor {

    // Per-datasource slow query segregation managers
    private final Map<String, SlowQuerySegregationManager> slowQuerySegregationManagers = new ConcurrentHashMap<>();
    private final ServerConfiguration serverConfiguration;

    @Override
    public void preProcessConnect(CurrentRequestContext<ConnectionDetails, StreamObserver<SessionInfo>> context) {
        String connHash = context.getConnHash();
        context.setCurrentSlowQuerySegregationManager(getSlowQuerySegregationManagerForConnection(connHash));
    }

    /**
     * Creates a slow query segregation manager for a specific datasource.
     * Each datasource gets its own manager with pool size based on actual HikariCP configuration.
     *
     * @return
     */
    private SlowQuerySegregationManager createSlowQuerySegregationManagerForDatasource(String connHash, int actualPoolSize) {
        SlowQuerySegregationManager manager = new SlowQuerySegregationManager(
                actualPoolSize,
                serverConfiguration.getSlowQuerySlotPercentage(),
                serverConfiguration.getSlowQueryIdleTimeout(),
                serverConfiguration.getSlowQuerySlowSlotTimeout(),
                serverConfiguration.getSlowQueryFastSlotTimeout(),
                true
        );
        slowQuerySegregationManagers.put(connHash, manager);
        log.info("Created SlowQuerySegregationManager for datasource {} with pool size {}",
                connHash, actualPoolSize);
        return manager;
    }

    /**
     * Gets the slow query segregation manager for a specific connection hash.
     * If no manager exists, creates a disabled one as a fallback.
     */
    public SlowQuerySegregationManager getSlowQuerySegregationManagerForConnection(String connHash) {
        SlowQuerySegregationManager manager = slowQuerySegregationManagers.get(connHash);
        if (manager == null) {
            context
            manager = this.createSlowQuerySegregationManagerForDatasource(connHash, serverConfiguration.get());
        }
        return manager;
    }


}
