package org.openjdbcproxy.grpc.server.service;

import com.openjdbcproxy.grpc.SessionInfo;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.grpc.server.*;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataSourceManager {




    private final ServerConfiguration serverConfiguration;
    private final SessionManager sessionManager;




    public Connection getConnection(SessionInfo sessionInfo) throws SQLException {
        return sessionManager.getConnection(sessionInfo);
    }

    public Connection createConnection(SessionInfo sessionInfo) throws SQLException {
        // Get the datasource for this connection hash
        HikariDataSource dataSource = this.datasourceMap.get(sessionInfo.getConnHash());
        if (dataSource == null) {
            throw new SQLException("No datasource found for connection hash: " + sessionInfo.getConnHash());
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


    /**
     * Finds a suitable connection for the current sessionInfo.
     * If there is a connection already in the sessionInfo reuse it, if not get a fresh one from the data source.
     *
     * @param sessionInfo        - current sessionInfo object.
     * @param startSessionIfNone - if true will start a new sessionInfo if none exists.
     * @return ConnectionSessionDTO
     * @throws SQLException if connection not found or closed (by timeout or other reason)
     */
    public SessionContext acquireSessionContext(SessionInfo sessionInfo, boolean startSessionIfNone) throws SQLException {

        SessionContext sessionContext = new SessionContext();
        if (StringUtils.isEmpty(sessionInfo.getSessionUUID())) {
            Connection conn = this.createConnection(sessionInfo);

            if (startSessionIfNone) {
                sessionInfo = sessionManager.createSession(sessionInfo.getClientUUID(), conn);
            }
        }
        sessionContext.setSessionInfo(sessionInfo);
        return sessionContext;
    }

}
