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

    // 추천 점수 계산에 쓰이는 재무 지표. 외부 데이터에서 채워지며, 없으면 0으로 취급.
    @Column(name = "per")
    private Double per;

    @Column(name = "pbr")
    private Double pbr;

    @Column(name = "roe")
    private Double roe;

    @Column(name = "dividend_yield")
    private Double dividendYield;

    protected Stock() {
    }

    public Stock(String code, String name) {
        this.code = code;
        this.name = name;
        this.watchCount = 0L;
    }

    public static Stock of(String code, String name, double per, double pbr, double roe, double dividendYield) {
        Stock stock = new Stock(code, name);
        stock.updateMetrics(per, pbr, roe, dividendYield);
        return stock;
    }

    public void updateMetrics(double per, double pbr, double roe, double dividendYield) {
        this.per = per;
        this.pbr = pbr;
        this.roe = roe;
        this.dividendYield = dividendYield;
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

    public double getPer() {
        return per == null ? 0.0 : per;
    }

    public double getPbr() {
        return pbr == null ? 0.0 : pbr;
    }

    public double getRoe() {
        return roe == null ? 0.0 : roe;
    }

    public double getDividendYield() {
        return dividendYield == null ? 0.0 : dividendYield;
    }
}
