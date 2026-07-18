package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.domain.MarketType;

/**
 * 종목코드를 Yahoo Finance 심볼로 변환한다.
 * 명시적 override 매핑이 있으면 우선하고, 없으면 시장에 따라 접미사를 붙인다.
 * - KOSPI → {code}.KS, KOSDAQ → {code}.KQ, 미국(NASDAQ/NYSE) → {code}
 */
public class YahooSymbolResolver {

    private final YahooProperties properties;

    public YahooSymbolResolver(YahooProperties properties) {
        this.properties = properties;
    }

    public String resolve(String code, MarketType market) {
        String override = properties.getSymbols().get(code);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return switch (market) {
            case KOSPI -> code + ".KS";
            case KOSDAQ -> code + ".KQ";
            case NASDAQ, NYSE -> code;
        };
    }
}
