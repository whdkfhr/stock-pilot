package com.arok2.stockpilot.price.stream;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PriceStreamConsumerTest {

    @Mock
    private PriceStreamService priceStreamService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void 시세이벤트를_틱으로_변환해_브로드캐스트한다() throws Exception {
        PriceStreamConsumer consumer = new PriceStreamConsumer(priceStreamService, objectMapper);
        String payload = objectMapper.writeValueAsString(
                new StockPriceEvent("000660", 130000, 900, Instant.now(), 128000));

        consumer.consume(payload);

        ArgumentCaptor<PriceTick> captor = ArgumentCaptor.forClass(PriceTick.class);
        verify(priceStreamService).broadcast(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("000660");
        assertThat(captor.getValue().change()).isEqualTo(2000);
    }

    @Test
    void 잘못된_페이로드는_예외없이_무시한다() {
        PriceStreamConsumer consumer = new PriceStreamConsumer(priceStreamService, objectMapper);

        consumer.consume("not-json");

        verifyNoInteractions(priceStreamService);
    }
}
