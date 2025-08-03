package com.exchange.config;

import com.exchange.model.LeadPosition;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        configureSerializer(template);
        return template;
    }

    @Bean
    public RedisTemplate<String, LeadPosition> leadPositionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, LeadPosition> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<LeadPosition> serializer = new Jackson2JsonRedisSerializer<>(LeadPosition.class);
        template.setDefaultSerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        return template;
    }


    private void configureSerializer(RedisTemplate<?, ?> template) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setDefaultSerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
    }
}
