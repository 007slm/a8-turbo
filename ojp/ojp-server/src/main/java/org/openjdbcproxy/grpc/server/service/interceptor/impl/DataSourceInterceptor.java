package org.openjdbcproxy.grpc.server.service.interceptor.impl;

import com.openjdbcproxy.grpc.ConnectionDetails;
import com.openjdbcproxy.grpc.SessionInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.grpc.server.ConnectionAcquisitionManager;
import org.openjdbcproxy.grpc.server.service.interceptor.context.CurrentRequestContext;
import org.openjdbcproxy.grpc.server.service.interceptor.StatementServiceInterceptor;
import org.openjdbcproxy.grpc.server.pool.ConnectionPoolConfigurer;
import org.openjdbcproxy.grpc.server.utils.ConnectionHashGenerator;
import org.openjdbcproxy.grpc.server.utils.UrlParser;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断器拦截器，实现与业务代码中相同的熔断器逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceInterceptor implements StatementServiceInterceptor {
    private final Map<String, HikariDataSource> datasourceMap = new ConcurrentHashMap<>();


    public HikariDataSource buildDatasource(ConnectionDetails connectionDetails,String connHash){
        HikariDataSource ds = this.datasourceMap.get(connHash);
        if (ds == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(UrlParser.parseUrl(connectionDetails.getUrl()));
            config.setUsername(connectionDetails.getUser());
            config.setPassword(connectionDetails.getPassword());

            // Configure HikariCP using client properties or defaults
            ConnectionPoolConfigurer.configureHikariPool(config, connectionDetails);

            ds = new HikariDataSource(config);
            this.datasourceMap.put(connHash, ds);

            // 统一延迟到 获取连接的时候 再创建需要的 慢查询管理器
            // createSlowQuerySegregationManagerForDatasource(connHash, config.getMaximumPoolSize());
        }
        return ds;
    }

    @Override
    public  void preProcessConnect(CurrentRequestContext<ConnectionDetails, StreamObserver<SessionInfo>> context) {
        ConnectionDetails connectionDetails = context.getRequest();
        String connHash = ConnectionHashGenerator.hashConnectionDetails(connectionDetails);
        log.info("connect connHash = {}", connHash);
        context.setConnHash(connHash);
        HikariDataSource ds = this.buildDatasource(connectionDetails,connHash);
        context.setCurrentDataSource(ds);
        context.setDataSourceInterceptor(this);
    }

    public Connection createConnection(CurrentRequestContext requestContext) throws SQLException {
        // Get the datasource for this connection hash
        HikariDataSource dataSource =  requestContext.getCurrentDataSource();
        String connHash = requestContext.getConnHash();
        if (dataSource == null) {
            throw new SQLException("No datasource found for connection hash: " + connHash );
        }
        Connection conn;
        try {
            // Use enhanced connection acquisition with timeout protection
            conn = ConnectionAcquisitionManager.acquireConnection(dataSource, sessionInfo.getConnHash());
            log.debug("Successfully acquired connection from pool for hash: {}", sessionInfo.getConnHash());
        } catch (SQLException e) {
            log.error("Failed to acquire connection from pool for hash: {}. Error: {}",
                    sessionInfo.getConnHash(), e.getMessage());

            // Re-throw the enhanced exception from ConnectionAcquisitionManager
            throw e;
        }
        return conn;
    }

}
