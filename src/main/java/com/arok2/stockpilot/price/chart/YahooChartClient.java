package com.arok2.stockpilot.price.chart;

import com.arok2.stockpilot.price.YahooHttp;
import com.arok2.stockpilot.price.quote.QuoteMeta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Yahoo Finance 과거 시세(캔들)·요약 조회. 시장 데이터라 시세 소스 설정과 무관하게 Yahoo에서 직접 가져온다.
 * 실패 시 빈 목록/null을 반환한다(차트·요약이 없어도 화면이 동작해야 함).
 */
@Component
public class YahooChartClient {

    private static final Logger log = LoggerFactory.getLogger(YahooChartClient.class);
    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final String USER_AGENT = "Mozilla/5.0 (StockPilot demo)";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YahooChartClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .requestFactory(YahooHttp.timeoutFactory())
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
        this.objectMapper = objectMapper;
    }

    public List<ChartPoint> fetch(String symbol, String range, String interval) {
        try {
            return parse(get(symbol, range, interval));
        } catch (Exception e) {
            log.warn("Yahoo 차트 조회 실패 (symbol={} range={}): {}", symbol, range, e.getMessage());
            return List.of();
        }
    }

    /** 종목 요약(고가/저가/거래량/52주 최고·최저/이름/거래소). 실패 시 null. */
    public QuoteMeta fetchMeta(String symbol) {
        try {
            return parseMeta(get(symbol, "1d", "5m"));
        } catch (Exception e) {
            log.warn("Yahoo 요약 조회 실패 (symbol={}): {}", symbol, e.getMessage());
            return null;
        }
    }

    private String get(String symbol, String range, String interval) {
        return restClient.get()
                .uri("/v8/finance/chart/{symbol}?range={range}&interval={interval}", symbol, range, interval)
                .retrieve()
                .body(String.class);
    }

    /** chart 응답에서 OHLCV 봉을 추출한다. close가 null인 봉은 건너뛴다. */
    List<ChartPoint> parse(String json) {
        try {
            JsonNode result = objectMapper.readTree(json).path("chart").path("result").path(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").path(0);
            JsonNode closes = quote.path("close");
            if (!timestamps.isArray() || !closes.isArray()) {
                return List.of();
            }
            JsonNode opens = quote.path("open");
            JsonNode highs = quote.path("high");
            JsonNode lows = quote.path("low");
            JsonNode volumes = quote.path("volume");
            List<ChartPoint> points = new ArrayList<>();
            for (int i = 0; i < timestamps.size() && i < closes.size(); i++) {
                JsonNode close = closes.get(i);
                if (close == null || close.isNull()) {
                    continue;
                }
                double c = close.asDouble();
                points.add(new ChartPoint(
                        Instant.ofEpochSecond(timestamps.get(i).asLong()),
                        num(opens, i, c), num(highs, i, c), num(lows, i, c), c,
                        volumes.path(i).asLong(0)));
            }
            return points;
        } catch (Exception e) {
            log.warn("Yahoo 차트 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    QuoteMeta parseMeta(String json) {
        try {
            JsonNode meta = objectMapper.readTree(json).path("chart").path("result").path(0).path("meta");
            if (meta.isMissingNode()) {
                return null;
            }
            return new QuoteMeta(
                    optDouble(meta, "regularMarketDayHigh"),
                    optDouble(meta, "regularMarketDayLow"),
                    meta.path("regularMarketVolume").asLong(0),
                    optDouble(meta, "fiftyTwoWeekHigh"),
                    optDouble(meta, "fiftyTwoWeekLow"),
                    meta.path("longName").asText(meta.path("shortName").asText(null)),
                    meta.path("exchangeName").asText(null));
        } catch (Exception e) {
            log.warn("Yahoo 메타 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private static double num(JsonNode arr, int i, double fallback) {
        JsonNode n = arr.path(i);
        return n.isNumber() ? n.asDouble() : fallback;
    }

    private static Double optDouble(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isNumber() ? n.asDouble() : null;
    }
}
