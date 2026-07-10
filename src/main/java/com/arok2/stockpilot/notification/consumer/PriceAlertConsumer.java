package com.arok2.stockpilot.notification.consumer;

import com.arok2.stockpilot.notification.service.AlertEvaluator;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * `stock-price` 토픽을 소비해 사용자의 가격 알림 조건을 평가한다. (consumer group: notification)
 * price-cache / price-analytics와 독립적으로 같은 이벤트를 소비한다(하나의 이벤트, 다수 Consumer).
 */
@Component
public class PriceAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertConsumer.class);

    private final AlertEvaluator alertEvaluator;
    private final ObjectMapper objectMapper;

    public PriceAlertConsumer(AlertEvaluator alertEvaluator, ObjectMapper objectMapper) {
        this.alertEvaluator = alertEvaluator;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${stockpilot.kafka.stock-price-topic:stock-price}", groupId = "notification")
    public void consume(String payload) {
        try {
            StockPriceEvent event = objectMapper.readValue(payload, StockPriceEvent.class);
            alertEvaluator.evaluate(event);
        } catch (Exception e) {
            log.warn("알림 조건 평가 실패 (payload={}): {}", payload, e.getMessage());
        }
    }
}
