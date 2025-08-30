package org.openjdbcproxy.grpc.server.service.interceptor.impl;

import com.openjdbcproxy.grpc.ConnectionDetails;
import com.openjdbcproxy.grpc.DbName;
import com.openjdbcproxy.grpc.SessionInfo;
import io.grpc.stub.StreamObserver;
import org.openjdbcproxy.database.DatabaseUtils;
import org.openjdbcproxy.grpc.server.service.interceptor.context.CurrentRequestContext;
import org.openjdbcproxy.grpc.server.service.interceptor.StatementServiceInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DB2ResourceMetadataInterceptor implements StatementServiceInterceptor {

    private final Map<String, DbName> dbNameMap = new ConcurrentHashMap<>();

    @Override
    public void preProcessConnect(CurrentRequestContext<ConnectionDetails, StreamObserver<SessionInfo>> context) {
        String connHash = context.getConnHash();
        this.dbNameMap.put(connHash, DatabaseUtils.resolveDbName(context.getRequest().getUrl()));
    }


}
