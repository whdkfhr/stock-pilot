package com.arok2.stockpilot.price.stream;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * `stock-price` 토픽을 소비해 SSE 구독자에게 실시간 틱을 push한다. (consumer group: price-stream)
 * price-cache/price-analytics/notification과 독립적으로 같은 이벤트를 소비한다.
 */
@Component
public class PriceStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceStreamConsumer.class);

    private final PriceStreamService priceStreamService;
    private final ObjectMapper objectMapper;

    public PriceStreamConsumer(PriceStreamService priceStreamService, ObjectMapper objectMapper) {
        this.priceStreamService = priceStreamService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${stockpilot.kafka.stock-price-topic:stock-price}", groupId = "price-stream")
    public void consume(String payload) {
        try {
            StockPriceEvent event = objectMapper.readValue(payload, StockPriceEvent.class);
            priceStreamService.broadcast(PriceTick.from(event));
        } catch (Exception e) {
            log.warn("SSE 틱 브로드캐스트 실패 (payload={}): {}", payload, e.getMessage());
        }
    }
}
