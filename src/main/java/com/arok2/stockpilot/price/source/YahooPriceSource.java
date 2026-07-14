package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Yahoo Finance 실시간(약 15분 지연) 시세 소스. {@code stockpilot.price.source=yahoo}일 때 활성화된다.
 * 공개 chart 엔드포인트(v8)를 호출해 최신 체결가/거래량을 읽어 시세 이벤트로 변환한다.
 * 비공식 API이므로 실패는 상위(수집 스케줄러)에서 종목 단위로 격리·스킵한다.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.source", havingValue = "yahoo")
public class YahooPriceSource implements PriceSource {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    // 비공식 엔드포인트는 브라우저 User-Agent가 없으면 종종 차단된다.
    private static final String USER_AGENT = "Mozilla/5.0 (StockPilot demo)";

    private final RestClient restClient;
    private final YahooSymbolResolver symbolResolver;
    private final ObjectMapper objectMapper;

    public YahooPriceSource(RestClient.Builder restClientBuilder,
                            YahooProperties properties,
                            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
        this.symbolResolver = new YahooSymbolResolver(properties);
        this.objectMapper = objectMapper;
    }

    @Override
    public StockPriceEvent fetch(String code) {
        String symbol = symbolResolver.resolve(code);
        String body = restClient.get()
                .uri("/v8/finance/chart/{symbol}?interval=1d&range=1d", symbol)
                .retrieve()
                .body(String.class);
        return parse(body, code);
    }

    /** Yahoo chart 응답에서 최신가/거래량을 추출한다. (테스트 가능하도록 분리) */
    StockPriceEvent parse(String json, String code) {
        try {
            JsonNode meta = objectMapper.readTree(json)
                    .path("chart").path("result").path(0).path("meta");
            if (meta.isMissingNode() || !meta.has("regularMarketPrice")) {
                throw new IllegalStateException("시세 필드 없음");
            }
            long price = Math.round(meta.path("regularMarketPrice").asDouble());
            long volume = meta.path("regularMarketVolume").asLong(0);
            return new StockPriceEvent(code, price, volume, Instant.now());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Yahoo 시세 파싱 실패 (" + code + "): " + e.getMessage(), e);
        }
    }
}
