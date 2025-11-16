package com.example.shopservice.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Explicitly registers the primary DataSource backed by spring.datasource.* so
 * that JPA works with shopdb, while the Chinook dataset can keep its own
 * dedicated connection.
 */
@Configuration
public class ShopDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties shopDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource shopDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
