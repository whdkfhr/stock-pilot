package com.arok2.stockpilot.price.domain;

import com.arok2.stockpilot.price.event.StockPriceEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "price_history",
        indexes = @Index(name = "idx_price_history_code_traded_at", columnList = "code, traded_at")
)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private long volume;

    @Column(name = "traded_at", nullable = false)
    private Instant tradedAt;

    protected PriceHistory() {
        // JPA 기본 생성자
    }

    private PriceHistory(String code, long price, long volume, Instant tradedAt) {
        this.code = code;
        this.price = price;
        this.volume = volume;
        this.tradedAt = tradedAt;
    }

    public static PriceHistory from(StockPriceEvent event) {
        return new PriceHistory(event.code(), event.price(), event.volume(), event.timestamp());
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public long getPrice() {
        return price;
    }

    public long getVolume() {
        return volume;
    }

    public Instant getTradedAt() {
        return tradedAt;
    }
}
