package com.redis.smartcache.cli;

import com.redis.lettucemod.RedisModulesClient;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.smartcache.core.ClientManager;
import com.redis.smartcache.core.config.Config;
import com.redis.smartcache.core.config.RedisConfig;

public class RedisConfigurator {

    public RedisConfigurator(String hostname, String port, String applicationName){
        this.hostname = hostname;
        this.port = port;
        this.applicationName = applicationName;
    }

    private final String hostname;
    private final String port;

    public String getApplicationName() {
        return applicationName;
    }

    private final String applicationName;


    public Config conf(){
        Config config = new Config();
        RedisConfig redisConfig = new RedisConfig();
        redisConfig.setUri(String.format("redis://%s:%s",hostname,port));
        config.setRedis(redisConfig);
        config.setName(applicationName);
        return config;
    }

    public ClientManager abstractRedisClient(){
        return new ClientManager();
    }


    public StatefulRedisModulesConnection<String, String> modClient(){
        return RedisModulesClient.create(String.format("redis://%s:%s",hostname,port)).connect();
    }
}