package com.arok2.stockpilot.notification.domain;

/**
 * 가격 알림 조건의 방향.
 * - ABOVE: 시세가 임계값 이상이 되면 발화
 * - BELOW: 시세가 임계값 이하가 되면 발화
 */
public enum AlertDirection {
    ABOVE,
    BELOW
}
