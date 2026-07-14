package com.arok2.stockpilot.price.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YahooSymbolResolverTest {

    @Test
    void 매핑이_없으면_기본_접미사를_붙인다() {
        YahooProperties props = new YahooProperties(); // defaultSuffix=.KS
        YahooSymbolResolver resolver = new YahooSymbolResolver(props);

        assertThat(resolver.resolve("005930")).isEqualTo("005930.KS");
    }

    @Test
    void 명시적_매핑이_있으면_그것을_우선한다() {
        YahooProperties props = new YahooProperties();
        props.getSymbols().put("086520", "086520.KQ"); // KOSDAQ
        YahooSymbolResolver resolver = new YahooSymbolResolver(props);

        assertThat(resolver.resolve("086520")).isEqualTo("086520.KQ");
        assertThat(resolver.resolve("005930")).isEqualTo("005930.KS"); // 매핑 없는 건 기본 접미사
    }

    @Test
    void 기본_접미사를_KOSDAQ로_바꿀_수_있다() {
        YahooProperties props = new YahooProperties();
        props.setDefaultSuffix(".KQ");
        YahooSymbolResolver resolver = new YahooSymbolResolver(props);

        assertThat(resolver.resolve("086520")).isEqualTo("086520.KQ");
    }
}
