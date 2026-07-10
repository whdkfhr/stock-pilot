package com.arok2.stockpilot.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 알림 조건이 충족되어 생성된 알림. 조건 발화 시 Consumer가 적재하고,
 * 사용자는 자신의 알림 목록을 조회한다.
 */
@Entity
@Table(
        name = "notification",
        indexes = @Index(name = "idx_notification_user_created", columnList = "user_id, created_at")
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private long price;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA 기본 생성자
    }

    private Notification(Long userId, String stockCode, String message, long price) {
        this.userId = userId;
        this.stockCode = stockCode;
        this.message = message;
        this.price = price;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public static Notification of(Long userId, String stockCode, String message, long price) {
        return new Notification(userId, stockCode, message, price);
    }

    public void markRead() {
        this.read = true;
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

    public String getMessage() {
        return message;
    }

    public long getPrice() {
        return price;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
