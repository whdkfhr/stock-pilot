package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.domain.MarketType;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.price.YahooHttp;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yahoo Finance 시세 조회 로직(재사용 가능). 시세 소스 설정과 무관하게 항상 빈으로 존재하며,
 * 야후 소스뿐 아니라 KIS 하이브리드(미국 종목)에서도 사용한다.
 * 가격은 통화의 정수 단위(원/달러)로 반올림한다(미국 종목은 센트 손실 허용).
 */
@Component
public class YahooPriceFetcher {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final String USER_AGENT = "Mozilla/5.0 (StockPilot demo)";

    private final RestClient restClient;
    private final YahooSymbolResolver symbolResolver;
    private final ObjectMapper objectMapper;
    private final StockRepository stockRepository;
    private final Map<String, MarketType> marketCache = new ConcurrentHashMap<>();

    public YahooPriceFetcher(RestClient.Builder restClientBuilder,
                             YahooProperties properties,
                             ObjectMapper objectMapper,
                             StockRepository stockRepository) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .requestFactory(YahooHttp.timeoutFactory())
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
        this.symbolResolver = new YahooSymbolResolver(properties);
        this.objectMapper = objectMapper;
        this.stockRepository = stockRepository;
    }

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

    /** Yahoo chart 응답에서 최신가(통화 정수 단위)/거래량/전일종가를 추출한다. (테스트 가능하도록 분리) */
    StockPriceEvent parse(String json, String code) {
        try {
            JsonNode meta = objectMapper.readTree(json)
                    .path("chart").path("result").path(0).path("meta");
            if (meta.isMissingNode() || !meta.has("regularMarketPrice")) {
                throw new IllegalStateException("시세 필드 없음");
            }
            long price = Math.round(meta.path("regularMarketPrice").asDouble());
            long volume = meta.path("regularMarketVolume").asLong(0);
            double prevCloseRaw = meta.path("previousClose").asDouble(
                    meta.path("chartPreviousClose").asDouble(price));
            long previousClose = Math.round(prevCloseRaw);
            return new StockPriceEvent(code, price, volume, Instant.now(), previousClose);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Yahoo 시세 파싱 실패 (" + code + "): " + e.getMessage(), e);
        }
    }
}
