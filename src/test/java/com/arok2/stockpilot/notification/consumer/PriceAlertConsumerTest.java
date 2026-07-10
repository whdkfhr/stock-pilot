package com.arok2.stockpilot.notification.consumer;

import com.arok2.stockpilot.notification.service.AlertEvaluator;
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
class PriceAlertConsumerTest {

    @Mock
    private AlertEvaluator alertEvaluator;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void 시세이벤트를_역직렬화해_평가기로_위임한다() throws Exception {
        PriceAlertConsumer consumer = new PriceAlertConsumer(alertEvaluator, objectMapper);
        String payload = objectMapper.writeValueAsString(
                new StockPriceEvent("005930", 61000, 1000, Instant.now()));

        consumer.consume(payload);

        ArgumentCaptor<StockPriceEvent> captor = ArgumentCaptor.forClass(StockPriceEvent.class);
        verify(alertEvaluator).evaluate(captor.capture());
        assertThat(captor.getValue().code()).isEqualTo("005930");
        assertThat(captor.getValue().price()).isEqualTo(61000);
    }

    @Test
    void 잘못된_페이로드는_예외를_던지지_않고_무시한다() {
        PriceAlertConsumer consumer = new PriceAlertConsumer(alertEvaluator, objectMapper);

        consumer.consume("not-a-json");

        verifyNoInteractions(alertEvaluator);
    }
}
