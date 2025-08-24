package io.nats.demo;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class LoggingListener {

    @Bean
    public Consumer<String> onDataChange() {
        return cdc -> {
            String confirmation = "收到订单 " + cdc + "";
            System.out.println(confirmation);
        };
    }



}
