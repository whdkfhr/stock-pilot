package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.domain.MarketType;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yahoo Finance 실시간(약 15분 지연) 시세 소스. {@code stockpilot.price.source=yahoo}일 때 활성화된다.
 * 종목의 시장(KOSPI/KOSDAQ/미국)에 맞는 Yahoo 심볼로 chart 엔드포인트를 호출한다.
 * 가격은 통화의 정수 단위(원/달러)로 반올림해 저장한다(미국 종목은 센트 단위 손실 허용).
 * 비공식 API이므로 실패는 상위(수집 스케줄러)에서 종목 단위로 격리·스킵한다.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.source", havingValue = "yahoo")
public class YahooPriceSource implements PriceSource {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final String USER_AGENT = "Mozilla/5.0 (StockPilot demo)";

    private final RestClient restClient;
    private final YahooSymbolResolver symbolResolver;
    private final ObjectMapper objectMapper;
    private final StockRepository stockRepository;
    // 종목코드 → 시장 캐시(반복 DB 조회 방지). 종목 집합은 거의 바뀌지 않는다.
    private final Map<String, MarketType> marketCache = new ConcurrentHashMap<>();

    public YahooPriceSource(RestClient.Builder restClientBuilder,
                            YahooProperties properties,
                            ObjectMapper objectMapper,
                            StockRepository stockRepository) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
        this.symbolResolver = new YahooSymbolResolver(properties);
        this.objectMapper = objectMapper;
        this.stockRepository = stockRepository;
    }

    @Override
    public StockPriceEvent fetch(String code) {
        MarketType market = marketCache.computeIfAbsent(code, this::lookupMarket);
        String symbol = symbolResolver.resolve(code, market);
        String body = restClient.get()
                .uri("/v8/finance/chart/{symbol}?interval=1d&range=1d", symbol)
                .retrieve()
                .body(String.class);
        return parse(body, code);
    }

    private MarketType lookupMarket(String code) {
        return stockRepository.findByCode(code).map(Stock::getMarket).orElse(MarketType.KOSPI);
    }

    /** Yahoo chart 응답에서 최신가(통화 정수 단위)/거래량을 추출한다. (테스트 가능하도록 분리) */
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
