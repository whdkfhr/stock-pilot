package com.arok2.stockpilot.domain;

/**
 * 종목이 상장된 시장. Yahoo 심볼 접미사와 통화가 시장에 따라 결정된다.
 * - KOSPI  : {code}.KS, KRW
 * - KOSDAQ : {code}.KQ, KRW
 * - NASDAQ / NYSE : {code}(접미사 없음), USD
 */
public enum MarketType {
    KOSPI("KRW"),
    KOSDAQ("KRW"),
    NASDAQ("USD"),
    NYSE("USD");

    private final String currency;

    MarketType(String currency) {
        this.currency = currency;
    }

    public String currency() {
        return currency;
    }

    public boolean isDomestic() {
        return this == KOSPI || this == KOSDAQ;
    }
}
