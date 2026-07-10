package com.arok2.stockpilot.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(Long alertId) {
        super("알림 조건을 찾을 수 없습니다: " + alertId);
    }
}
