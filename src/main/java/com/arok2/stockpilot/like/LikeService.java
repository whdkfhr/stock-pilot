package com.arok2.stockpilot.like;

import com.arok2.stockpilot.observability.StockPilotMetrics;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 종목 좋아요. Redis Set(stock:likes:{code})에 사용자 id를 담아 원자적으로 처리한다.
 * - SADD는 원자적이며 중복 사용자는 무시된다(1인 1좋아요).
 * - 좋아요 수는 SCARD로 항상 일관되게 조회된다(동시 요청에도 정확).
 * DB는 클릭마다 건드리지 않고, 주기적 배치(LikeSyncScheduler)로만 동기화한다.
 */
@Service
public class LikeService {

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public LikeService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    /** 좋아요 등록 후 현재 좋아요 수 반환. */
    public long like(Long userId, String code) {
        redisTemplate.opsForSet().add(key(code), String.valueOf(userId));
        meterRegistry.counter(StockPilotMetrics.LIKE_REGISTERED).increment();
        return count(code);
    }

    public long count(String code) {
        Long size = redisTemplate.opsForSet().size(key(code));
        return size == null ? 0L : size;
    }

    private String key(String code) {
        return "stock:likes:" + code;
    }
}
