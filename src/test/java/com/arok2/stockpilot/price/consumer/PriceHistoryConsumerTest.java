package com.arok2.stockpilot.price.consumer;

import com.arok2.stockpilot.price.domain.PriceHistory;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.price.repository.PriceHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceHistoryConsumerTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void 시세이벤트를_받아_이력으로_저장한다() throws Exception {
        PriceHistoryConsumer consumer = new PriceHistoryConsumer(priceHistoryRepository, objectMapper);
        String payload = objectMapper.writeValueAsString(
                new StockPriceEvent("005930", 57000, 1000, Instant.now()));

        consumer.consume(payload);

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("005930");
        assertThat(captor.getValue().getPrice()).isEqualTo(57000);
        assertThat(captor.getValue().getVolume()).isEqualTo(1000);
    }
}
