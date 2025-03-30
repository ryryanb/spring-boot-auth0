package com.auth0.example;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private static final Logger LOGGER = Logger.getLogger(RedisConfig.class.getName());

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            LettuceConnectionFactory factory = new LettuceConnectionFactory();
            factory.afterPropertiesSet(); // Ensure the factory is initialized
            return factory;
        } catch (RedisConnectionFailureException e) {
            LOGGER.log(Level.SEVERE, "⚠️ Redis connection failed! Running in fallback mode.", e);
            return null; // Return null to signal Redis is unavailable
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        if (connectionFactory == null) {
            LOGGER.warning("⚠️ Redis is unavailable. RedisTemplate will not be functional.");
            return template; // Return a non-configured template to avoid crashes
        }

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
