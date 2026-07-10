package com.arok2.stockpilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 사용자가 등록한 가격 알림 조건. 예: "삼성전자(005930) 60,000원 이상".
 * 시세 이벤트가 들어올 때마다 ACTIVE 조건을 평가하고, 충족되면 한 번만 발화(TRIGGERED)한다.
 * 발화 전이는 원자적 UPDATE(ACTIVE→TRIGGERED)로 수행되어 동시 이벤트에도 알림이 중복 생성되지 않는다.
 */
@Entity
@Table(
        name = "alert_condition",
        indexes = @Index(name = "idx_alert_code_status", columnList = "stock_code, status")
)
public class AlertCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertDirection direction;

    @Column(nullable = false)
    private long threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    protected AlertCondition() {
        // JPA 기본 생성자
    }

    private AlertCondition(Long userId, String stockCode, AlertDirection direction, long threshold) {
        this.userId = userId;
        this.stockCode = stockCode;
        this.direction = direction;
        this.threshold = threshold;
        this.status = AlertStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public static AlertCondition of(Long userId, String stockCode, AlertDirection direction, long threshold) {
        return new AlertCondition(userId, stockCode, direction, threshold);
    }

    /** 주어진 시세가 이 조건을 충족하는지 판단한다. */
    public boolean matches(long price) {
        return switch (direction) {
            case ABOVE -> price >= threshold;
            case BELOW -> price <= threshold;
        };
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getStockCode() {
        return stockCode;
    }

    public AlertDirection getDirection() {
        return direction;
    }

    public long getThreshold() {
        return threshold;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }
}
