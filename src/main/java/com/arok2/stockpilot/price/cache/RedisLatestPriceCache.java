package com.arok2.stockpilot.price.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 최신가를 Redis String 키(stock:price:{code})에 저장한다.
 */
@Component
public class RedisLatestPriceCache implements LatestPriceCache {

    private static final String KEY_PREFIX = "stock:price:";

    private final StringRedisTemplate redisTemplate;

    public RedisLatestPriceCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void update(String code, long price) {
        redisTemplate.opsForValue().set(KEY_PREFIX + code, String.valueOf(price));
    }

    @Override
    public Long get(String code) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + code);
        return value == null ? null : Long.valueOf(value);
    }
}
