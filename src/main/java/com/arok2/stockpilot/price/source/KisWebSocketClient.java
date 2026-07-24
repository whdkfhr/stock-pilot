package com.arok2.stockpilot.price.source;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.price.event.StockPriceEvent;
import com.arok2.stockpilot.price.producer.PriceProducer;
import com.arok2.stockpilot.repository.StockRepository;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * KIS 실시간 체결가(H0STCNT0) WebSocket 구독. {@code stockpilot.kis.websocket-enabled=true}일 때 활성화.
 * 국내 종목을 구독해 체결 틱을 Kafka(stock-price)로 발행 → 기존 파이프라인(캐시/SSE/알림) 재사용.
 * 이 경우 REST 폴링(수집 스케줄러)은 국내를 건너뛴다(중복/rate 방지).
 *
 * 프레임 포맷(실측): {@code 0|H0STCNT0|<건수>|<데이터>}, 데이터는 ^로 구분되며 레코드당 46필드 반복.
 * 레코드 필드(0-base): 0=종목코드, 2=현재가, 4=전일대비(부호포함), 13=누적거래량.
 */
@Component
@ConditionalOnProperty(name = "stockpilot.kis.websocket-enabled", havingValue = "true")
public class KisWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketClient.class);
    private static final int FIELDS_PER_RECORD = 46;
    private static final int F_CODE = 0, F_PRICE = 2, F_PRDY_VRSS = 4, F_ACML_VOL = 13;

    private final KisProperties properties;
    private final KisApprovalClient approvalClient;
    private final StockRepository stockRepository;
    private final PriceProducer priceProducer;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kis-ws");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private volatile WebSocket webSocket;
    private volatile String approvalKey; // 24h 유효 → 재연결 시 재사용(approval 재발급 남용 방지)
    private volatile boolean running = true;

    public KisWebSocketClient(KisProperties properties,
                              KisApprovalClient approvalClient,
                              StockRepository stockRepository,
                              PriceProducer priceProducer) {
        this.properties = properties;
        this.approvalClient = approvalClient;
        this.stockRepository = stockRepository;
        this.priceProducer = priceProducer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        connect();
    }

    @PreDestroy
    public void stop() {
        running = false;
        scheduler.shutdownNow();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    private void scheduleReconnect() {
        if (running) {
            scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    private synchronized void connect() {
        if (!running || !connecting.compareAndSet(false, true)) {
            return;
        }
        try {
            List<String> codes = stockRepository.findAll().stream()
                    .filter(s -> s.getMarket().isDomestic())
                    .map(Stock::getCode)
                    .toList();
            if (codes.isEmpty()) {
                return;
            }
            if (approvalKey == null) {
                approvalKey = approvalClient.getApprovalKey();
            }
            WebSocket ws = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(properties.getWsUrl()), new Listener())
                    .get(10, TimeUnit.SECONDS);
            this.webSocket = ws;
            for (String code : codes) {
                ws.sendText(subscribeMessage(code, approvalKey), true);
                Thread.sleep(80); // 구독 버스트 방지
            }
            log.info("KIS WebSocket 체결가 구독 시작: {}종목", codes.size());
        } catch (Exception e) {
            log.warn("KIS WebSocket 연결 실패, 재시도 예정: {}", e.getMessage());
            scheduleReconnect();
        } finally {
            connecting.set(false);
        }
    }

    private String subscribeMessage(String code, String approval) {
        return "{\"header\":{\"approval_key\":\"" + approval
                + "\",\"custtype\":\"P\",\"tr_type\":\"1\",\"content-type\":\"utf-8\"},"
                + "\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"" + code + "\"}}}";
    }

    /** 체결 프레임에서 마지막 레코드를 시세 이벤트로 변환. 제어 메시지/형식 불일치는 empty. */
    static Optional<StockPriceEvent> parseFrame(String text) {
        if (text == null || text.isEmpty() || text.charAt(0) == '{') {
            return Optional.empty(); // JSON 제어 메시지(구독 응답/PINGPONG)
        }
        String[] parts = text.split("\\|", 4);
        if (parts.length < 4 || !"H0STCNT0".equals(parts[1])) {
            return Optional.empty();
        }
        try {
            int count = Integer.parseInt(parts[2].trim());
            String[] f = parts[3].split("\\^");
            int base = (count - 1) * FIELDS_PER_RECORD; // 가장 최근 체결
            if (count < 1 || f.length < base + F_ACML_VOL + 1) {
                return Optional.empty();
            }
            String code = f[base + F_CODE];
            long price = Long.parseLong(f[base + F_PRICE].trim());
            long prdyVrss = Long.parseLong(f[base + F_PRDY_VRSS].trim()); // 부호 포함
            long volume = Long.parseLong(f[base + F_ACML_VOL].trim());
            long previousClose = price - prdyVrss;
            return Optional.of(new StockPriceEvent(code, price, volume, Instant.now(), previousClose));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String text = buffer.toString();
                buffer.setLength(0);
                if (text.startsWith("{") && text.contains("PINGPONG")) {
                    ws.sendText(text, true); // PINGPONG은 그대로 되돌려 연결 유지
                } else {
                    parseFrame(text).ifPresent(priceProducer::publish);
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("KIS WebSocket 종료({}) — 재연결 예정", statusCode);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.warn("KIS WebSocket 오류 — 재연결 예정: {}", error.getMessage());
            scheduleReconnect();
        }
    }
}
