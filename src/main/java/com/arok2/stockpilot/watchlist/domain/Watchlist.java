package com.arok2.stockpilot.watchlist.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "watchlist",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_watchlist_user_stock",
                columnNames = {"user_id", "stock_id"}
        )
)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Watchlist() {
    }

    private Watchlist(Long userId, Long stockId) {
        if (userId == null || stockId == null) {
            throw new IllegalArgumentException("관심종목은 사용자와 종목이 모두 필요합니다");
        }
        this.userId = userId;
        this.stockId = stockId;
        this.createdAt = Instant.now();
    }

    /** 사용자가 종목을 관심등록한다. 등록 시각은 도메인이 정한다. */
    public static Watchlist register(Long userId, Long stockId) {
        return new Watchlist(userId, stockId);
    }

    /** 이 관심등록이 해당 사용자의 것인지. (소유자만 해제할 수 있다) */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    /** 같은 종목에 대한 관심등록인지. */
    public boolean isFor(Long stockId) {
        return this.stockId.equals(stockId);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getStockId() {
        return stockId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
