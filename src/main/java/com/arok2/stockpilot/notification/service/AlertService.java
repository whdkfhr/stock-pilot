package com.arok2.stockpilot.notification.service;

import com.arok2.stockpilot.exception.AlertNotFoundException;
import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.dto.AlertCreateRequest;
import com.arok2.stockpilot.notification.dto.AlertResponse;
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
    public AlertResponse create(Long userId, AlertCreateRequest request) {
        AlertCondition saved = alertConditionRepository.save(
                AlertCondition.of(userId, request.stockCode(), request.direction(), request.threshold()));
        return AlertResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getMyAlerts(Long userId) {
        return alertConditionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long alertId) {
        AlertCondition alert = alertConditionRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        alertConditionRepository.delete(alert);
    }
}
