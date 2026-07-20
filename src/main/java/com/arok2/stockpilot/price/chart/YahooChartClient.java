package com.arok2.stockpilot.price.chart;

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
 * Yahoo Finance 과거 시세(캔들) 조회. 시장 데이터라 시세 소스 설정과 무관하게 Yahoo에서 직접 가져온다.
 * 실패 시 빈 목록을 반환한다(차트는 없어도 화면이 동작해야 함).
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
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
        this.objectMapper = objectMapper;
    }

    public List<ChartPoint> fetch(String symbol, String range, String interval) {
        try {
            String body = restClient.get()
                    .uri("/v8/finance/chart/{symbol}?range={range}&interval={interval}",
                            symbol, range, interval)
                    .retrieve()
                    .body(String.class);
            return parse(body);
        } catch (Exception e) {
            log.warn("Yahoo 차트 조회 실패 (symbol={} range={}): {}", symbol, range, e.getMessage());
            return List.of();
        }
    }

    /** chart 응답에서 (timestamp, close) 쌍을 추출한다. close가 null인 구간은 건너뛴다. */
    List<ChartPoint> parse(String json) {
        try {
            JsonNode result = objectMapper.readTree(json).path("chart").path("result").path(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode closes = result.path("indicators").path("quote").path(0).path("close");
            if (!timestamps.isArray() || !closes.isArray()) {
                return List.of();
            }
            List<ChartPoint> points = new ArrayList<>();
            for (int i = 0; i < timestamps.size() && i < closes.size(); i++) {
                JsonNode close = closes.get(i);
                if (close == null || close.isNull()) {
                    continue;
                }
                points.add(new ChartPoint(
                        Instant.ofEpochSecond(timestamps.get(i).asLong()), close.asDouble()));
            }
            return points;
        } catch (Exception e) {
            log.warn("Yahoo 차트 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
