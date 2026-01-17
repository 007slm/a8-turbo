package org.openjdbcproxy.grpc.server;

import org.openjdbcproxy.grpc.SqlErrorResponse;
import org.openjdbcproxy.grpc.SqlErrorType;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * Handles exceptions that need to be reported via GRPC.
 */
@Slf4j
public class GrpcExceptionHandler {

    /**
     * Handles the reporting or SQLExceptions.
     * 
     * @param e              SQLException
     * @param streamObserver target stream observer.
     * @param <T>            Stream observer generic type.
     */
    public static <T> void sendSQLExceptionMetadata(SQLException e, StreamObserver<T> streamObserver) {
        sendSQLExceptionMetadata(e, streamObserver, SqlErrorType.SQL_EXCEPTION);
    }

    /**
     * Handles the reporting or SQLExceptions.
     * 
     * @param e              SQLException
     * @param streamObserver target stream observer.
     * @param <T>            Stream observer generic type.
     * @param sqlErrorType   Indicates the type of error.
     */
    public static <T> void sendSQLExceptionMetadata(SQLException e, StreamObserver<T> streamObserver,
            SqlErrorType sqlErrorType) {
        sendSQLExceptionMetadata(e, streamObserver, sqlErrorType, Status.CANCELLED);
    }

    public static <T> void sendSQLExceptionMetadata(SQLException e, StreamObserver<T> streamObserver,
            SqlErrorType sqlErrorType, Status status) {
        try {
            Metadata metadata = new Metadata();

            SqlErrorResponse.Builder responseBuilder = SqlErrorResponse.newBuilder()
                    .setReason(e.getMessage())
                    .setSqlErrorType(sqlErrorType)
                    .setVendorCode(e.getErrorCode());
            if (e.getSQLState() != null) {
                responseBuilder.setSqlState(e.getSQLState());
            }

            SqlErrorResponse sqlErrorResponse = responseBuilder.build();
            Metadata.Key<SqlErrorResponse> errorResponseKey = ProtoUtils
                    .keyForProto(SqlErrorResponse.getDefaultInstance());
            metadata.put(errorResponseKey, sqlErrorResponse);

            // Important: Use description to prevent "Received unexpected EOS" error on some
            // clients
            // when just Metadata is sent with certain statuses.
            //streamObserver.onError(Status.CANCELLED.asRuntimeException(metadata));
            streamObserver.onError(status.withDescription(e.getMessage()).asRuntimeException(metadata));
        } catch (RuntimeException re) {
            log.error("Failed while sending error to client: " + re.getMessage() + ": " + e.getMessage(), e);
            // Fallback to simple error if metadata construction fails
            streamObserver.onError(Status.INTERNAL.withDescription("Failed to report SQL Exception: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
