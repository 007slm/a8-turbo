package org.openjdbcproxy.grpc.server.utils;

import com.openjdbcproxy.grpc.ConnectionDetails;

public class JdbcUrlUtil {

    public static String connHash(ConnectionDetails connectionDetails) {
        return connectionDetails.getUrl();
    }
    
    public static String extractDatabaseName(String connHash) {
        int lastSlashIndex = connHash.lastIndexOf('/');
        if (lastSlashIndex == -1 || lastSlashIndex == connHash.length() - 1) {
            return null;
        }
        String dbPart = connHash.substring(lastSlashIndex + 1);
        int paramIndex = dbPart.indexOf('?');
        if (paramIndex != -1) {
            return dbPart.substring(0, paramIndex);
        } else {
            return dbPart;
        }
    }
}