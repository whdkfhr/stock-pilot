package com.arok2.stockpilot.price.consumer;

import com.arok2.stockpilot.price.domain.PriceHistory;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.price.repository.PriceHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * `stock-price` 토픽을 소비해 PostgreSQL에 시세 이력을 적재한다. (consumer group: price-analytics)
 * 같은 토픽을 price-cache와 독립적으로 소비한다(하나의 이벤트, 다수 Consumer).
 */
@Component
public class PriceHistoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceHistoryConsumer.class);

    private final PriceHistoryRepository priceHistoryRepository;
    private final ObjectMapper objectMapper;

    public PriceHistoryConsumer(PriceHistoryRepository priceHistoryRepository, ObjectMapper objectMapper) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${stockpilot.kafka.stock-price-topic:stock-price}", groupId = "price-analytics")
    public void consume(String payload) {
        try {
            StockPriceEvent event = objectMapper.readValue(payload, StockPriceEvent.class);
            priceHistoryRepository.save(PriceHistory.from(event));
        } catch (Exception e) {
            log.warn("시세 이력 적재 실패 (payload={}): {}", payload, e.getMessage());
        }
    }
}
