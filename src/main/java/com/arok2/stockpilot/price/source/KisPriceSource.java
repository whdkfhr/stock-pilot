package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.domain.MarketType;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.price.YahooHttp;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국투자증권(KIS) 실시간 시세 소스(하이브리드). {@code stockpilot.price.source=kis}일 때 활성화된다.
 * - 국내(KOSPI/KOSDAQ): KIS 국내주식 현재가 조회(실시간·무지연).
 * - 미국(NASDAQ/NYSE): {@link YahooPriceFetcher} 사용(KIS 해외 실시간은 별도 신청 필요).
 * 실패는 상위(수집 스케줄러)에서 종목 단위로 격리·스킵한다.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.source", havingValue = "kis")
public class KisPriceSource implements PriceSource {

    private static final String TR_ID_DOMESTIC_PRICE = "FHKST01010100";
    // KIS 초당 호출 제한(EGW00201) 회피용 최소 간격. 수집기는 단일 스레드 순차 호출이다.
    // 실전 지속 폴링은 rate가 빡빡해 일부 스킵은 불가피(자가치유). ~1.6/s로 제한.
    // 다수 종목 실시간의 정석은 REST 폴링이 아니라 WebSocket 체결가 구독이다(다음 단계).
    private static final long MIN_GAP_MS = 600;

    private final Object throttleLock = new Object();
    private long lastCallMs = 0;

    private final RestClient restClient;
    private final KisTokenClient tokenClient;
    private final ObjectMapper objectMapper;
    private final StockRepository stockRepository;
    private final YahooPriceFetcher yahooPriceFetcher;
    private final Map<String, MarketType> marketCache = new ConcurrentHashMap<>();

    public KisPriceSource(KisProperties properties,
                          KisTokenClient tokenClient,
                          ObjectMapper objectMapper,
                          StockRepository stockRepository,
                          YahooPriceFetcher yahooPriceFetcher) {
        this.tokenClient = tokenClient;
        this.objectMapper = objectMapper;
        this.stockRepository = stockRepository;
        this.yahooPriceFetcher = yahooPriceFetcher;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(YahooHttp.timeoutFactory())
                .defaultHeader("appkey", properties.getAppKey())
                .defaultHeader("appsecret", properties.getAppSecret())
                .defaultHeader("custtype", "P")
                .build();
    }

    @Override
    public StockPriceEvent fetch(String code) {
        MarketType market = marketCache.computeIfAbsent(code, this::lookupMarket);
        if (!market.isDomestic()) {
            return yahooPriceFetcher.fetch(code); // 미국 종목은 야후
        }
        // KIS는 지속 폴링 시 초당제한(EGW00201)을 확률적으로 반환한다. 재시도는 부하를 키워 역효과이므로
        // 한 번만 호출하고, 실패한 종목은 다음 주기에 갱신되도록 둔다(자가치유). rate 실패는 조용히 표시.
        throttle();
        try {
            return parseDomestic(requestDomestic(code), code);
        } catch (RestClientResponseException e) {
            if (e.getMessage() != null && e.getMessage().contains("EGW00201")) {
                throw new KisRateLimitException(code);
            }
            throw e;
        }
    }

    private String requestDomestic(String code) {
        return restClient.get()
                .uri("/uapi/domestic-stock/v1/quotations/inquire-price"
                        + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD={code}", code)
                .header("authorization", "Bearer " + tokenClient.getToken())
                .header("tr_id", TR_ID_DOMESTIC_PRICE)
                .retrieve()
                .body(String.class);
    }

    private MarketType lookupMarket(String code) {
        return stockRepository.findByCode(code).map(Stock::getMarket).orElse(MarketType.KOSPI);
    }

    /** KIS 초당 호출 제한을 넘지 않도록 호출 간 최소 간격을 유지한다. */
    private void throttle() {
        synchronized (throttleLock) {
            long wait = MIN_GAP_MS - (System.currentTimeMillis() - lastCallMs);
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastCallMs = System.currentTimeMillis();
        }
    }

    /** KIS 국내 현재가 응답 파싱. output.stck_prpr(현재가)/acml_vol(거래량)/prdy_vrss(전일대비)+부호. */
    StockPriceEvent parseDomestic(String json, String code) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!"0".equals(root.path("rt_cd").asText())) {
                throw new IllegalStateException("KIS 오류: " + root.path("msg1").asText());
            }
            JsonNode o = root.path("output");
            long price = asLong(o.path("stck_prpr").asText());
            long volume = asLong(o.path("acml_vol").asText());
            long vrss = asLong(o.path("prdy_vrss").asText());
            String sign = o.path("prdy_vrss_sign").asText("2"); // 1상한 2상승 3보합 4하한 5하락
            long signedChange = (sign.equals("4") || sign.equals("5")) ? -vrss : vrss;
            long previousClose = price - signedChange;
            return new StockPriceEvent(code, price, volume, Instant.now(), previousClose);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("KIS 시세 파싱 실패 (" + code + "): " + e.getMessage(), e);
        }
    }

    private static long asLong(String s) {
        if (s == null || s.isBlank()) return 0;
        return Math.round(Double.parseDouble(s.trim()));
    }
}
