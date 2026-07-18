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

    // Redis 좋아요 카운트를 주기적으로 동기화한 값(정합성 스냅샷). null이면 0으로 취급.
    @Column(name = "like_count")
    private Long likeCount;

    // 상장 시장. 기존 행 호환을 위해 nullable, null이면 KOSPI로 취급.
    @Enumerated(EnumType.STRING)
    @Column(name = "market")
    private MarketType market;

    protected Stock() {
    }

    public Stock(String code, String name) {
        this.code = code;
        this.name = name;
        this.watchCount = 0L;
        this.market = MarketType.KOSPI;
    }

    public static Stock of(String code, String name, double per, double pbr, double roe, double dividendYield) {
        return of(code, name, MarketType.KOSPI, per, pbr, roe, dividendYield);
    }

    public static Stock of(String code, String name, MarketType market,
                           double per, double pbr, double roe, double dividendYield) {
        Stock stock = new Stock(code, name);
        stock.market = market;
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

    public long getLikeCount() {
        return likeCount == null ? 0L : likeCount;
    }

    public void updateLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    /** 상장 시장(기존 데이터 호환: null이면 KOSPI). */
    public MarketType getMarket() {
        return market == null ? MarketType.KOSPI : market;
    }

    public void updateMarket(MarketType market) {
        this.market = market;
    }
}
