package com.arok2.stockpilot.domain;

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

    public Watchlist(Long userId, Long stockId) {
        this.userId = userId;
        this.stockId = stockId;
        this.createdAt = Instant.now();
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
