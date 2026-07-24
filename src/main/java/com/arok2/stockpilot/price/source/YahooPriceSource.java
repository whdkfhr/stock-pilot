package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Yahoo Finance 시세 소스. {@code stockpilot.price.source=yahoo}일 때 활성화된다.
 * 실제 조회 로직은 {@link YahooPriceFetcher}에 있다(KIS 하이브리드와 공유).
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.source", havingValue = "yahoo")
public class YahooPriceSource implements PriceSource {

    private final YahooPriceFetcher fetcher;

    public YahooPriceSource(YahooPriceFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public StockPriceEvent fetch(String code) {
        return fetcher.fetch(code);
    }
}
