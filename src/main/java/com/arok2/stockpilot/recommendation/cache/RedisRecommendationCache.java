package com.arok2.stockpilot.recommendation.cache;

import com.arok2.stockpilot.recommendation.dto.RecommendationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 추천 결과를 Redis에 JSON으로 캐싱한다. 키: user:{id}:recommend, TTL 60초.
 */
@Component
public class RedisRecommendationCache implements RecommendationCache {

    private static final Logger log = LoggerFactory.getLogger(RedisRecommendationCache.class);
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRecommendationCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RecommendationResponse get(Long userId) {
        String json = redisTemplate.opsForValue().get(key(userId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RecommendationResponse.class);
        } catch (Exception e) {
            log.warn("추천 캐시 역직렬화 실패(캐시 미스로 처리) userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public void put(Long userId, RecommendationResponse response) {
        try {
            redisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(response), TTL);
        } catch (Exception e) {
            // 캐시 저장 실패는 무시한다 — 계산 결과는 이미 호출자에게 반환된다.
            log.warn("추천 캐시 저장 실패 userId={}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void evict(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return "user:" + userId + ":recommend";
    }
}
