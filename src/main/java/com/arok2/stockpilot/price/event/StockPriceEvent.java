package com.arok2.stockpilot.price.event;

import java.time.Instant;

/**
 * 실시간 시세 이벤트. Kafka `stock-price` 토픽에 JSON으로 발행된다.
 * 파티션 키로 종목코드(code)를 사용해 동일 종목의 순서를 보장한다.
 * previousClose(전일 종가)는 등락 계산에 쓰인다.
 */
public record StockPriceEvent(
        String code,
        long price,
        long volume,
        Instant timestamp,
        long previousClose
) {
    /** previousClose를 모를 때는 현재가와 동일(등락 0)로 둔다. */
    public StockPriceEvent(String code, long price, long volume, Instant timestamp) {
        this(code, price, volume, timestamp, price);
    }
}
