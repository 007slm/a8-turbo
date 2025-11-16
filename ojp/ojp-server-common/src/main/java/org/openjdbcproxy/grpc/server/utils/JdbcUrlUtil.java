package org.openjdbcproxy.grpc.server.utils;

import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.grpc.ConnectionDetails;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class JdbcUrlUtil {

    public static String connHash(ConnectionDetails connectionDetails) {
        if (connectionDetails == null) {
            return "";
        }
        String url = StringUtils.defaultString(connectionDetails.getUrl(), "");
        String username = connectionDetails.getUser();
        String password = connectionDetails.getPassword();
        if (StringUtils.isBlank(url) || StringUtils.isBlank(username)) {
            return url;
        }
        if (containsInlineCredentials(url)) {
            return url;
        }
        String encodedUser = encode(username);
        String encodedPassword = StringUtils.isNotBlank(password) ? encode(password) : null;
        StringBuilder credentials = new StringBuilder(encodedUser);
        if (StringUtils.isNotBlank(encodedPassword)) {
            credentials.append(':').append(encodedPassword);
        }
        credentials.append('@');

        String marker = "_mysql://";
        int markerIndex = url.indexOf(marker);
        if (markerIndex >= 0) {
            int insertionPoint = markerIndex + marker.length();
            return new StringBuilder(url.length() + credentials.length())
                    .append(url, 0, insertionPoint)
                    .append(credentials)
                    .append(url.substring(insertionPoint))
                    .toString();
        }
        int schemeIndex = url.indexOf("://");
        if (schemeIndex >= 0) {
            int insertionPoint = schemeIndex + 3;
            return new StringBuilder(url.length() + credentials.length())
                    .append(url, 0, insertionPoint)
                    .append(credentials)
                    .append(url.substring(insertionPoint))
                    .toString();
        }
        String separator = url.contains("?") ? "&" : "?";
        StringBuilder builder = new StringBuilder(url)
                .append(separator)
                .append("user=").append(encodedUser);
        if (StringUtils.isNotBlank(encodedPassword)) {
            builder.append("&password=").append(encodedPassword);
        }
        return builder.toString();
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

    private static boolean containsInlineCredentials(String url) {
        int markerIndex = url.indexOf("_mysql://");
        int startIndex;
        if (markerIndex >= 0) {
            startIndex = markerIndex + "_mysql://".length();
        } else {
            int schemeIndex = url.indexOf("://");
            if (schemeIndex < 0) {
                return false;
            }
            startIndex = schemeIndex + 3;
        }
        int atIndex = url.indexOf('@', startIndex);
        if (atIndex < 0) {
            return false;
        }
        int slashIndex = url.indexOf('/', startIndex);
        int questionIndex = url.indexOf('?', startIndex);
        int boundary = url.length();
        if (slashIndex >= 0) {
            boundary = Math.min(boundary, slashIndex);
        }
        if (questionIndex >= 0) {
            boundary = Math.min(boundary, questionIndex);
        }
        return atIndex < boundary;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static Credentials extractCredentials(URI uri) {
        String userInfo = uri.getUserInfo();
        String username = null;
        String password = null;
        if (StringUtils.isNotBlank(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = parts[0];
            if (parts.length > 1) {
                password = parts[1];
            }
        }
        if (username == null || password == null) {
            String query = uri.getQuery();
            if (StringUtils.isNotBlank(query)) {
                for (String pair : query.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length != 2) {
                        continue;
                    }
                    String key = kv[0];
                    String value = kv[1];
                    if ("user".equalsIgnoreCase(key) || "username".equalsIgnoreCase(key)) {
                        username = decode(value);
                    } else if ("password".equalsIgnoreCase(key) || "pwd".equalsIgnoreCase(key)) {
                        password = decode(value);
                    }
                }
            }
        }
        return new Credentials(username, password);
    }

    private static String decode(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public record Credentials(String username, String password) {
    }
}
