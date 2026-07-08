package com.arok2.stockpilot.price.producer;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 시세 이벤트를 Kafka `stock-price` 토픽에 JSON 문자열로 발행한다.
 * 파티션 키는 종목코드(code) — 동일 종목 이벤트의 순서를 보장한다.
 */
@Component
public class PriceProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public PriceProducer(KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         @Value("${stockpilot.kafka.stock-price-topic:stock-price}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(StockPriceEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.code(), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("시세 이벤트 직렬화 실패: " + event.code(), e);
        }
    }
}
