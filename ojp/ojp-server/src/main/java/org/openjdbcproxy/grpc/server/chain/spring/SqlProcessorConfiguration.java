package org.openjdbcproxy.grpc.server.chain.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.openjdbcproxy.grpc.server.chain.SqlProcessor;
import org.openjdbcproxy.grpc.server.chain.SqlProcessorChain;


import java.util.List;
import java.util.Comparator;

/**
 * SQL处理器Spring配置类
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = "org.openjdbcproxy.grpc.server.chain.processors")
public class SqlProcessorConfiguration {
    
    @Autowired
    private List<SqlProcessor> sqlProcessors;
    
    /**
     * 创建SQL处理责任链（单例）
     */
    @Bean
    public SqlProcessorChain sqlProcessorChain() {
        SqlProcessorChain chain = new SqlProcessorChain();
        
        sqlProcessors.forEach(chain::addProcessor);
        
        chain.buildChain();
        log.info("SQL processor chain created with {} processors", sqlProcessors.size());
        return chain;
    }
}
