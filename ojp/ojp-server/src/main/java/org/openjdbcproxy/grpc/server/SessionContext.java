package org.openjdbcproxy.grpc.server;

import com.openjdbcproxy.grpc.DbName;
import com.openjdbcproxy.grpc.SessionInfo;
import lombok.Data;
import org.openjdbcproxy.database.DatabaseUtils;

import java.sql.Connection;
import java.sql.SQLException;

@Data
public class SessionContext {
    private Connection connection;
    private SessionInfo sessionInfo;
    private DbName dbName;
    private SessionManager sessionManager;

    public void setSessionInfo(SessionInfo sessionInfo) throws SQLException {
        this.sessionInfo = sessionInfo;
        Connection conn = sessionManager.getConnection(sessionInfo);
        this.setDbName(DatabaseUtils.resolveDbName(conn.getMetaData().getURL()));
        this.setConnection(conn);
    }

}
