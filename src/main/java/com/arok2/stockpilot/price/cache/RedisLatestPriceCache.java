package com.arok2.stockpilot.price.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 최신가/전일종가를 Redis String 키(stock:price:{code}, stock:prevclose:{code})에 저장한다.
 */
@Component
public class RedisLatestPriceCache implements LatestPriceCache {

    private static final String PRICE_PREFIX = "stock:price:";
    private static final String PREV_CLOSE_PREFIX = "stock:prevclose:";

    private final StringRedisTemplate redisTemplate;

    public RedisLatestPriceCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void update(String code, long price, long previousClose) {
        redisTemplate.opsForValue().set(PRICE_PREFIX + code, String.valueOf(price));
        redisTemplate.opsForValue().set(PREV_CLOSE_PREFIX + code, String.valueOf(previousClose));
    }

    @Override
    public Long get(String code) {
        return readLong(PRICE_PREFIX + code);
    }

    @Override
    public Long getPreviousClose(String code) {
        return readLong(PREV_CLOSE_PREFIX + code);
    }

    private Long readLong(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? null : Long.valueOf(value);
    }
}
