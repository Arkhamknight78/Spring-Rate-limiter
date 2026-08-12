package com.example.Rate.Limiter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedisConfig {
    private String host = "localhost";
    private int port = 6379;

    private int timeout = 2000;


    //java client library for redis
    //Lets java app communicate with redis server

    @Bean
    public JedisPool getJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setJmxEnabled(false);
        //keeps multiple conn ready to reuse
        //this helps in efficient use of resources and avoids frequent conn establishment
        poolConfig.setMaxIdle(10);
        poolConfig.setMaxTotal(50);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);


        return new JedisPool(poolConfig, host, port, timeout);

    }
}
