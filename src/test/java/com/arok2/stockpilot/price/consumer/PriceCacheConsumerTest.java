package com.arok2.stockpilot.price.consumer;

import com.arok2.stockpilot.price.cache.LatestPriceCache;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PriceCacheConsumerTest {

    @Mock
    private LatestPriceCache latestPriceCache;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void 시세이벤트를_받아_최신가_캐시를_갱신한다() throws Exception {
        PriceCacheConsumer consumer = new PriceCacheConsumer(latestPriceCache, objectMapper);
        String payload = objectMapper.writeValueAsString(
                new StockPriceEvent("000660", 130000, 900, Instant.now()));

        consumer.consume(payload);

        verify(latestPriceCache).update("000660", 130000);
    }

    @Test
    void 잘못된_페이로드는_예외를_던지지_않고_무시한다() {
        PriceCacheConsumer consumer = new PriceCacheConsumer(latestPriceCache, objectMapper);

        consumer.consume("not-a-json");

        verifyNoInteractions(latestPriceCache);
    }
}
