package com.incidentiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPooled jedis(@Value("${spring.data.redis.host}") String host,
                             @Value("${spring.data.redis.port}") int port) {
        return new JedisPooled(host, port);
    }
}
