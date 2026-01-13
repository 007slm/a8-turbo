package org.openjdbcproxy.grpc.client;

import org.openjdbcproxy.grpc.ConnectionDetails;
import org.openjdbcproxy.grpc.SessionInfo;
import org.openjdbcproxy.grpc.StatementServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.sql.SQLException;

import static org.openjdbcproxy.grpc.client.GrpcExceptionHandler.handle;

public class StatementGrpcClient {
    public static void main(String[] args) throws SQLException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        StatementServiceGrpc.StatementServiceBlockingStub stub
                = StatementServiceGrpc.newBlockingStub(channel);

        try {
            SessionInfo sessionInfo = stub.connect(ConnectionDetails.newBuilder()
                    .setUrl("jdbc:ojp_h2:~/test")
                    .setUser("sa")
                    .setPassword("").build());
            sessionInfo.getConnHash();
        } catch (StatusRuntimeException e) {
            handle(e);
        }
        channel.shutdown();
    }
}
