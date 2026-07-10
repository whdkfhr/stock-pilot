package com.arok2.stockpilot.notification.domain;

/**
 * 알림 조건 상태.
 * - ACTIVE: 감시 중(시세 이벤트마다 조건 평가 대상)
 * - TRIGGERED: 이미 발화되어 알림을 생성함(중복 발화 방지, 재평가 제외)
 */
public enum AlertStatus {
    ACTIVE,
    TRIGGERED
}
