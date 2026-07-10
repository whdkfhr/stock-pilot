package com.arok2.stockpilot.notification.service;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.domain.AlertDirection;
import com.arok2.stockpilot.notification.domain.AlertStatus;
import com.arok2.stockpilot.notification.domain.Notification;
import com.arok2.stockpilot.notification.repository.AlertConditionRepository;
import com.arok2.stockpilot.notification.repository.NotificationRepository;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 평가기의 H2 종단 검증: 원자적 전이 JPQL 실행, 알림 적재, 그리고
 * 이미 발화된 조건은 재발화하지 않는 멱등성을 확인한다.
 */
@SpringBootTest
class AlertEvaluatorIntegrationTest {

    @Autowired
    private AlertEvaluator alertEvaluator;

    @Autowired
    private AlertConditionRepository alertConditionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        alertConditionRepository.deleteAll();
        stockRepository.deleteAll();
        stockRepository.save(Stock.of("005930", "삼성전자", 12, 1.4, 15, 2.0));
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        alertConditionRepository.deleteAll();
        stockRepository.deleteAll();
    }

    private StockPriceEvent event(long price) {
        return new StockPriceEvent("005930", price, 1000, Instant.now());
    }

    @Test
    void 조건_충족시_알림이_적재되고_조건은_TRIGGERED로_전이된다() {
        AlertCondition saved = alertConditionRepository.save(
                AlertCondition.of(7L, "005930", AlertDirection.ABOVE, 60_000));

        alertEvaluator.evaluate(event(61_000));

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(7L);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getMessage()).contains("삼성전자");
        assertThat(alertConditionRepository.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(AlertStatus.TRIGGERED);
    }

    @Test
    void 한번_발화된_조건은_이후_이벤트에_재발화하지_않는다() {
        alertConditionRepository.save(AlertCondition.of(7L, "005930", AlertDirection.ABOVE, 60_000));

        alertEvaluator.evaluate(event(61_000));
        alertEvaluator.evaluate(event(62_000));
        alertEvaluator.evaluate(event(63_000));

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L)).hasSize(1);
    }

    @Test
    void 조건에_못미치는_시세는_알림을_만들지_않는다() {
        alertConditionRepository.save(AlertCondition.of(7L, "005930", AlertDirection.ABOVE, 60_000));

        alertEvaluator.evaluate(event(59_000));

        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(7L)).isEmpty();
    }
}
