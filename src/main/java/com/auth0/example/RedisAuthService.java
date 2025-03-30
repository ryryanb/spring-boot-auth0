package com.auth0.example;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAuthService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Fallback in-memory storage
    private final ConcurrentHashMap<String, Map<String, String>> localCache = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRATION = 900; // 15 minutes

    @Autowired
    public RedisAuthService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveSession(String userId, String accessToken, String refreshToken) {
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("accessToken", accessToken);
        sessionData.put("refreshToken", refreshToken);

        try {
            redisTemplate.opsForValue().set("session:" + userId, sessionData, TOKEN_EXPIRATION, TimeUnit.SECONDS);
        } catch (DataAccessException e) {
            System.err.println("⚠️ Redis is unavailable, falling back to in-memory cache.");
            localCache.put("session:" + userId, sessionData);
        }
    }

    public Map<String, String> getSession(String userId) {
        try {
            Map<String, String> sessionData = (Map<String, String>) redisTemplate.opsForValue().get("session:" + userId);
            if (sessionData != null) {
                return sessionData;
            }
        } catch (DataAccessException e) {
            System.err.println("⚠️ Redis is unavailable, retrieving from in-memory cache.");
        }
        return localCache.get("session:" + userId);
    }

    public void deleteSession(String userId) {
        try {
            redisTemplate.delete("session:" + userId);
        } catch (DataAccessException e) {
            System.err.println("⚠️ Redis is unavailable, removing from in-memory cache.");
            localCache.remove("session:" + userId);
        }
    }
}
