package com.auth0.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAuthService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisAuthService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final long TOKEN_EXPIRATION = 900; // 15 minutes

    public void saveSession(String userId, String accessToken, String refreshToken) {
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("accessToken", accessToken);
        sessionData.put("refreshToken", refreshToken);
        redisTemplate.opsForValue().set("session:" + userId, sessionData, TOKEN_EXPIRATION, TimeUnit.SECONDS);
    }

    public Map<String, String> getSession(String userId) {
        return (Map<String, String>) redisTemplate.opsForValue().get("session:" + userId);
    }

    public void deleteSession(String userId) {
        redisTemplate.delete("session:" + userId);
    }
}
