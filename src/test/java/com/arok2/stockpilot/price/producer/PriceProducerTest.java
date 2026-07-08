package com.arok2.stockpilot.price.producer;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void 시세이벤트를_종목코드_키로_토픽에_JSON_발행한다() {
        PriceProducer producer = new PriceProducer(kafkaTemplate, objectMapper, "stock-price");
        StockPriceEvent event = new StockPriceEvent("005930", 57000, 1200, Instant.parse("2026-01-01T00:00:00Z"));

        producer.publish(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("stock-price"), eq("005930"), payload.capture());
        assertThat(payload.getValue()).contains("\"code\":\"005930\"").contains("57000");
    }
}
