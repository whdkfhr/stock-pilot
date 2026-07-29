package com.arok2.stockpilot.notification.service.command;

import com.arok2.stockpilot.notification.domain.AlertDirection;

/** 가격 알림 조건 등록 유스케이스 입력. */
public record RegisterAlertCommand(
        Long userId,
        String stockCode,
        AlertDirection direction,
        long threshold
) {
}
