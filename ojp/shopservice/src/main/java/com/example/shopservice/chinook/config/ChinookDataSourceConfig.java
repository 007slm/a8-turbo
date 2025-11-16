package com.example.shopservice.chinook.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.Driver;

/**
 * Dedicated Chinook dataset data source for ad-hoc SQL exploration.
 */
@Configuration
public class ChinookDataSourceConfig {

    @Bean(name = "chinookDataSource")
    public DataSource chinookDataSource(
            @Value("${chinook.datasource.url}") String url,
            @Value("${chinook.datasource.username}") String username,
            @Value("${chinook.datasource.password}") String password,
            @Value("${chinook.datasource.driver-class-name}") String driverClassName
    ) throws ClassNotFoundException {

        @SuppressWarnings("unchecked")
        Class<? extends Driver> driverClass =
                (Class<? extends Driver>) Class.forName(driverClassName);

        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(driverClass);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    public JdbcTemplate chinookJdbcTemplate(
            @Qualifier("chinookDataSource") DataSource chinookDataSource
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(chinookDataSource);
        jdbcTemplate.setResultsMapCaseInsensitive(true);
        return jdbcTemplate;
    }
}
