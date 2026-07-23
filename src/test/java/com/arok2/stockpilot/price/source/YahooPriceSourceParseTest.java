package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yahoo chart 응답 파싱 검증(네트워크 없이). HTTP 호출과 분리된 parse()만 테스트한다.
 */
class YahooPriceSourceParseTest {

    private YahooPriceFetcher newSource() {
        return new YahooPriceFetcher(
                RestClient.builder(), new YahooProperties(), new ObjectMapper(),
                Mockito.mock(StockRepository.class));
    }

    @Test
    void chart_응답에서_최신가와_거래량을_추출한다() {
        String json = """
                {"chart":{"result":[{"meta":{
                  "currency":"KRW","symbol":"005930.KS",
                  "regularMarketPrice":61000.0,"regularMarketVolume":12345678}}],"error":null}}
                """;

        StockPriceEvent event = newSource().parse(json, "005930");

        assertThat(event.code()).isEqualTo("005930");
        assertThat(event.price()).isEqualTo(61000);
        assertThat(event.volume()).isEqualTo(12345678);
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void 소수_가격은_반올림된다() {
        String json = "{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":61499.6}}]}}";

        StockPriceEvent event = newSource().parse(json, "005930");

        assertThat(event.price()).isEqualTo(61500);
        assertThat(event.volume()).isEqualTo(0); // 거래량 없으면 0
    }

    @Test
    void 시세_필드가_없으면_예외를_던진다() {
        String json = "{\"chart\":{\"result\":[{\"meta\":{}}],\"error\":null}}";

        assertThatThrownBy(() -> newSource().parse(json, "005930"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 결과가_비어있으면_예외를_던진다() {
        String json = "{\"chart\":{\"result\":[],\"error\":\"Not Found\"}}";

        assertThatThrownBy(() -> newSource().parse(json, "BADCODE"))
                .isInstanceOf(IllegalStateException.class);
    }
}
