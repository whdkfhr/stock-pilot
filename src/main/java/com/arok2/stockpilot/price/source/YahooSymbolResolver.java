package com.arok2.stockpilot.price.source;

/**
 * 종목코드를 Yahoo Finance 심볼로 변환한다.
 * 명시적 매핑이 있으면 우선 사용하고, 없으면 기본 접미사를 붙인다.
 */
public class YahooSymbolResolver {

    private final YahooProperties properties;

    public YahooSymbolResolver(YahooProperties properties) {
        this.properties = properties;
    }

    public String resolve(String code) {
        String mapped = properties.getSymbols().get(code);
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        return code + properties.getDefaultSuffix();
    }
}
