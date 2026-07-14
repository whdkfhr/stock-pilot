package com.arok2.stockpilot.price.source;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Yahoo Finance 시세 소스 설정.
 * 종목코드 → Yahoo 심볼 매핑 규칙: {@code symbols}에 명시적 매핑이 있으면 그것을,
 * 없으면 {@code defaultSuffix}를 코드에 붙인다(예: 005930 → 005930.KS).
 * KOSPI는 {@code .KS}, KOSDAQ은 {@code .KQ} 접미사를 쓴다.
 */
@ConfigurationProperties(prefix = "stockpilot.price.yahoo")
public class YahooProperties {

    /** 명시적 매핑이 없을 때 코드에 붙일 기본 접미사(기본값 KOSPI). */
    private String defaultSuffix = ".KS";

    /** 종목코드 → Yahoo 심볼 예외 매핑(KOSDAQ 등). */
    private Map<String, String> symbols = new LinkedHashMap<>();

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    public void setDefaultSuffix(String defaultSuffix) {
        this.defaultSuffix = defaultSuffix;
    }

    public Map<String, String> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, String> symbols) {
        this.symbols = symbols;
    }
}
