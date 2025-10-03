package org.openjdbcproxy.cache.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "org.openjdbcproxy.cache.repository")
public class RedisRepositoryConfig {
}
