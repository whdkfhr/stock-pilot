package com.arok2.stockpilot.price.producer;

import com.arok2.stockpilot.price.source.KisRateLimitException;
import com.arok2.stockpilot.price.source.PriceSource;
import com.arok2.stockpilot.repository.StockRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주기적으로 등록된 종목들의 시세를 조회해 Kafka로 발행한다.
 * `stockpilot.price.collector.enabled=false`면 비활성(테스트 환경 등).
 */
@Component
@ConditionalOnProperty(name = "stockpilot.price.collector.enabled", havingValue = "true", matchIfMissing = true)
public class PriceCollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceCollectorScheduler.class);

    private final StockRepository stockRepository;
    private final PriceSource priceSource;
    private final PriceProducer priceProducer;
    // WebSocket이 국내 실시간을 담당하면 REST 폴링은 국내를 건너뛴다(중복/rate 방지).
    private final boolean wsHandlesDomestic;

    public PriceCollectorScheduler(StockRepository stockRepository,
                                   PriceSource priceSource,
                                   PriceProducer priceProducer,
                                   @Value("${stockpilot.kis.websocket-enabled:false}") boolean wsHandlesDomestic) {
        this.stockRepository = stockRepository;
        this.priceSource = priceSource;
        this.priceProducer = priceProducer;
        this.wsHandlesDomestic = wsHandlesDomestic;
    }

    @Scheduled(fixedDelayString = "${stockpilot.price.collector.interval-ms:2000}")
    public void collect() {
        var stocks = stockRepository.findAll();
        int published = 0;
        for (var stock : stocks) {
            if (wsHandlesDomestic && stock.getMarket().isDomestic()) {
                continue; // 국내는 WebSocket 체결가가 담당
            }
            try {
                priceProducer.publish(priceSource.fetch(stock.getCode()));
                published++;
            } catch (KisRateLimitException e) {
                // KIS 초당 제한 스킵 — 다음 주기에 갱신되는 정상 상황이라 조용히 처리.
                log.debug("시세 수집 스킵(rate) {}", stock.getCode());
            } catch (Exception e) {
                // 외부 시세 API의 일시 오류가 한 종목 때문에 전체 수집을 막지 않도록 격리한다.
                log.warn("시세 수집 실패 {}: {}", stock.getCode(), e.getMessage());
            }
        }
        if (published > 0) {
            log.debug("시세 {}종목 발행", published);
        }
    }
}
