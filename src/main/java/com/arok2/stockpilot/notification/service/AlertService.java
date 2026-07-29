package com.arok2.stockpilot.notification.service;

import com.arok2.stockpilot.exception.AlertNotFoundException;
import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.service.command.RegisterAlertCommand;
import com.arok2.stockpilot.notification.repository.AlertConditionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자의 가격 알림 조건 관리(등록/조회/삭제). 조건 평가·알림 생성은
 * 이벤트 경로({@link AlertEvaluator})에서 별도로 수행한다.
 */
@Service
public class AlertService {

    private final AlertConditionRepository alertConditionRepository;

    public AlertService(AlertConditionRepository alertConditionRepository) {
        this.alertConditionRepository = alertConditionRepository;
    }

    @Transactional
    public AlertCondition create(RegisterAlertCommand command) {
        return alertConditionRepository.save(AlertCondition.of(
                command.userId(), command.stockCode(), command.direction(), command.threshold()));
    }

    @Transactional(readOnly = true)
    public List<AlertCondition> getMyAlerts(Long userId) {
        return alertConditionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void delete(Long userId, Long alertId) {
        AlertCondition alert = alertConditionRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alertConditionRepository.delete(alert);
    }
}
