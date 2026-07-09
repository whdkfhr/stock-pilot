package com.arok2.stockpilot.price.event;

import java.time.Instant;

/**
 * 실시간 시세 이벤트. Kafka `stock-price` 토픽에 JSON으로 발행된다.
 * 파티션 키로 종목코드(code)를 사용해 동일 종목의 순서를 보장한다.
 */
public record StockPriceEvent(
        String code,
        long price,
        long volume,
        Instant timestamp
) {
}
