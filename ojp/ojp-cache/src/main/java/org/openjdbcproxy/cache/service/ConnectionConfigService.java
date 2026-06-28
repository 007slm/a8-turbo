package org.openjdbcproxy.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openjdbcproxy.cache.entity.ConnectionConfig;
import org.openjdbcproxy.cache.repository.ConnectionConfigRepository;
import org.openjdbcproxy.grpc.server.utils.JdbcUrlUtil;
import org.openjdbcproxy.grpc.server.utils.UrlParser;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionConfigService {

    private final ConnectionConfigRepository connectionConfigRepository;

    public void ensureConnectionConfig(String connHash) {
        if (StringUtils.isBlank(connHash)) {
            return;
        }

        connectionConfigRepository.findById(connHash).ifPresentOrElse(
                existing -> {
                    existing.setLastActiveTime(LocalDateTime.now());
                    connectionConfigRepository.save(existing);
                },
                () -> {
                    createConnectionConfig(connHash);
                });
    }

    private void createConnectionConfig(String connHash) {
        try {
            String realUrl = UrlParser.parseUrl(connHash.trim());
            ConnectionConfig.ConnectionConfigBuilder builder = ConnectionConfig.builder()
                    .id(connHash)
                    .lastActiveTime(LocalDateTime.now())
                    .url(realUrl);

            if (realUrl.contains(":mysql:") || realUrl.startsWith("mysql://")) {
                parseMysql(realUrl, builder);
            } else if (realUrl.contains(":oracle:") || realUrl.startsWith("oracle://")) {
                parseOracle(realUrl, builder);
            } else {
                builder.name("Unknown DB");
            }

            ConnectionConfig config = builder.build();
            connectionConfigRepository.save(config);
            log.info("Auto-created ConnectionConfig for: {}", connHash);
        } catch (Exception e) {
            log.warn("Failed to parse and create ConnectionConfig for hash: {}", connHash, e);
        }
    }

    private void parseMysql(String realUrl, ConnectionConfig.ConnectionConfigBuilder builder) {
        builder.dbType("MySQL");
        try {
            int mysqlIdx = realUrl.indexOf("mysql://");
            if (mysqlIdx >= 0) {
                String mysqlSection = realUrl.substring(mysqlIdx);
                URI uri = new URI(mysqlSection);

                builder.host(uri.getHost());
                builder.port(uri.getPort() > 0 ? uri.getPort() : 3306);

                JdbcUrlUtil.Credentials credentials = JdbcUrlUtil.extractCredentials(uri);
                builder.username(credentials.username());
                builder.password(credentials.password());

                String dbName = "Unknown DB";
                String path = uri.getPath();
                if (StringUtils.isNotBlank(path) && path.length() > 1) {
                    String db = path.substring(1);
                    int qIdx = db.indexOf('?');
                    if (qIdx >= 0)
                        db = db.substring(0, qIdx);
                    builder.databaseName(db);
                    dbName = db;
                }

                builder.name(
                        uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : 3306) + "/" + dbName);
            }
        } catch (Exception e) {
            log.warn("Error parsing MySQL URL: {}", realUrl);
        }
    }

    private void parseOracle(String realUrl, ConnectionConfig.ConnectionConfigBuilder builder) {
        builder.dbType("Oracle");
        try {
            String uriString;
            if (realUrl.startsWith("oracle://")) {
                uriString = realUrl;
            } else {
                int atIndex = realUrl.indexOf('@');
                if (atIndex < 0)
                    return;
                String hostPart = realUrl.substring(atIndex + 1);
                if (hostPart.contains(":") && !hostPart.contains("/")) {
                    int firstColon = hostPart.indexOf(':');
                    int secondColon = hostPart.indexOf(':', firstColon + 1);
                    if (secondColon >= 0) {
                        hostPart = hostPart.substring(0, secondColon) + "/" + hostPart.substring(secondColon + 1);
                    }
                }
                uriString = "oracle://" + hostPart;
            }

            URI uri = new URI(uriString);
            builder.host(uri.getHost());
            builder.port(uri.getPort() > 0 ? uri.getPort() : 1521);

            JdbcUrlUtil.Credentials credentials = JdbcUrlUtil.extractCredentials(uri);
            builder.username(credentials.username());
            builder.password(credentials.password());

            String path = uri.getPath();
            String serviceName = (path != null && path.length() > 1) ? path.substring(1) : "ORCL";
            builder.databaseName(serviceName);

            builder.name(uri.getHost() + ":" + (uri.getPort() > 0 ? uri.getPort() : 1521) + "/" + serviceName);

        } catch (Exception e) {
            log.warn("Error parsing Oracle URL: {}", realUrl);
        }
    }
}
