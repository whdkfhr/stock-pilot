package com.arok2.stockpilot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "watch_count", nullable = false)
    private Long watchCount = 0L;

    protected Stock() {
    }

    public Stock(String code, String name) {
        this.code = code;
        this.name = name;
        this.watchCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Long getWatchCount() {
        return watchCount;
    }
}
