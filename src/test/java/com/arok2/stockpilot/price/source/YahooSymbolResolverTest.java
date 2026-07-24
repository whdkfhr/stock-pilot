package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.domain.MarketType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YahooSymbolResolverTest {

    private final YahooSymbolResolver resolver = new YahooSymbolResolver(new YahooProperties());

    @Test
    void KOSPI는_KS_접미사를_붙인다() {
        assertThat(resolver.resolve("005930", MarketType.KOSPI)).isEqualTo("005930.KS");
    }

    @Test
    void KOSDAQ는_KQ_접미사를_붙인다() {
        assertThat(resolver.resolve("247540", MarketType.KOSDAQ)).isEqualTo("247540.KQ");
    }

    @Test
    void 미국_종목은_접미사가_없다() {
        assertThat(resolver.resolve("AAPL", MarketType.NASDAQ)).isEqualTo("AAPL");
        assertThat(resolver.resolve("KO", MarketType.NYSE)).isEqualTo("KO");
    }

    @Test
    void 명시적_override_매핑이_있으면_우선한다() {
        YahooProperties props = new YahooProperties();
        props.getSymbols().put("005930", "005930.KS-CUSTOM");
        YahooSymbolResolver r = new YahooSymbolResolver(props);

        assertThat(r.resolve("005930", MarketType.KOSPI)).isEqualTo("005930.KS-CUSTOM");
    }
}
