package com.arok2.stockpilot.price.consumer;

import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * `stock-price` 토픽을 소비해 Redis 최신가를 갱신한다. (consumer group: price-cache)
 */
@Component
public class PriceCacheConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceCacheConsumer.class);

    private final LatestPriceCache latestPriceCache;
    private final ObjectMapper objectMapper;

    public PriceCacheConsumer(LatestPriceCache latestPriceCache, ObjectMapper objectMapper) {
        this.latestPriceCache = latestPriceCache;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${stockpilot.kafka.stock-price-topic:stock-price}", groupId = "price-cache")
    public void consume(String payload) {
        try {
            StockPriceEvent event = objectMapper.readValue(payload, StockPriceEvent.class);
            latestPriceCache.update(event.code(), event.price());
        } catch (Exception e) {
            log.warn("최신가 갱신 실패 (payload={}): {}", payload, e.getMessage());
        }
    }
}
