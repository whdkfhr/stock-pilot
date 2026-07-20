package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 외부 API를 대체하는 데모용 시세 소스. 종목별 최근가를 기준으로 랜덤워크하여
 * 실시간 시세와 유사한 흐름을 생성한다. {@code stockpilot.price.source}가 없거나 random일 때 활성화(기본값).
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.source", havingValue = "random", matchIfMissing = true)
public class RandomWalkPriceSource implements PriceSource {

    private static final long DEFAULT_BASE = 50_000L;
    private static final long MIN_PRICE = 1_000L;

    private final Map<String, Long> lastPrice = new ConcurrentHashMap<>();
    // 세션 기준가(전일 종가 역할). 첫 조회 시 고정되어 등락이 이 값 대비로 누적된다.
    private final Map<String, Long> sessionOpen = new ConcurrentHashMap<>();

    @Override
    public StockPriceEvent fetch(String code) {
        long base = lastPrice.getOrDefault(code, DEFAULT_BASE);
        long previousClose = sessionOpen.computeIfAbsent(code, c -> base);
        long delta = ThreadLocalRandom.current().nextLong(-500, 501);
        long price = Math.max(MIN_PRICE, base + delta);
        lastPrice.put(code, price);
        long volume = ThreadLocalRandom.current().nextLong(100, 10_000);
        return new StockPriceEvent(code, price, volume, Instant.now(), previousClose);
    }
}
