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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 시세 이벤트를 받아 활성 알림 조건을 평가하고, 충족된 조건에 대해 알림을 생성한다.
 * 조건 발화는 원자적 UPDATE(ACTIVE→TRIGGERED)로 1회만 성공하므로 중복 알림이 생기지 않는다.
 * (이벤트 경로에서 호출되며, 컨트롤러/서비스 조회 경로와 분리되어 있다.)
 */
@Service
public class AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluator.class);

    private final AlertConditionRepository alertConditionRepository;
    private final NotificationRepository notificationRepository;
    private final StockRepository stockRepository;

    public AlertEvaluator(AlertConditionRepository alertConditionRepository,
                          NotificationRepository notificationRepository,
                          StockRepository stockRepository) {
        this.alertConditionRepository = alertConditionRepository;
        this.notificationRepository = notificationRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public void evaluate(StockPriceEvent event) {
        List<AlertCondition> candidates =
                alertConditionRepository.findByStockCodeAndStatus(event.code(), AlertStatus.ACTIVE);
        if (candidates.isEmpty()) {
            return;
        }

        String stockName = stockRepository.findByCode(event.code())
                .map(Stock::getName)
                .orElse(event.code());

        for (AlertCondition condition : candidates) {
            if (!condition.matches(event.price())) {
                continue;
            }
            // 원자적 전이: 이미 다른 이벤트가 발화시켰다면 0행 → 알림 생성 생략(중복 방지)
            int updated = alertConditionRepository.markTriggered(condition.getId(), Instant.now());
            if (updated == 1) {
                String message = buildMessage(stockName, condition, event.price());
                notificationRepository.save(
                        Notification.of(condition.getUserId(), event.code(), message, event.price()));
                log.debug("알림 발화 user={} {} price={}", condition.getUserId(), event.code(), event.price());
            }
        }
    }

    private String buildMessage(String stockName, AlertCondition condition, long price) {
        String directionLabel = condition.getDirection() == AlertDirection.ABOVE ? "이상" : "이하";
        return String.format("%s이(가) %,d원 %s이 되었습니다. (현재가 %,d원)",
                stockName, condition.getThreshold(), directionLabel, price);
    }
}
