package org.openjdbcproxy.grpc.server;

import org.openjdbcproxy.grpc.DbName;
import org.openjdbcproxy.grpc.SessionInfo;
import lombok.Builder;
import lombok.Getter;

import java.sql.Connection;

@Getter
@Builder
public class ConnectionSessionDTO {
    private Connection connection;
    private SessionInfo session;
    private DbName dbName;
}
