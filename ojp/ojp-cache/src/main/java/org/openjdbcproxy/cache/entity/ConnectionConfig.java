package org.openjdbcproxy.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("ojp:connection:config")
public class ConnectionConfig {
    @Id
    private String id; // connHash

    private String name;
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;

    private String cdcUsername;
    private String cdcPassword;

    // Add logic to store DB type if needed, or derive from other fields
    private String dbType;
    private String url;

    private LocalDateTime lastActiveTime;
}
