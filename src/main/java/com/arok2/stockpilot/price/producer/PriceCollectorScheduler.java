package com.arok2.stockpilot.price.producer;

import com.arok2.stockpilot.price.source.PriceSource;
import com.arok2.stockpilot.repository.StockRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public PriceCollectorScheduler(StockRepository stockRepository,
                                   PriceSource priceSource,
                                   PriceProducer priceProducer) {
        this.stockRepository = stockRepository;
        this.priceSource = priceSource;
        this.priceProducer = priceProducer;
    }

    @Scheduled(fixedDelayString = "${stockpilot.price.collector.interval-ms:2000}")
    public void collect() {
        var stocks = stockRepository.findAll();
        int published = 0;
        for (var stock : stocks) {
            try {
                priceProducer.publish(priceSource.fetch(stock.getCode()));
                published++;
            } catch (Exception e) {
                // 외부 시세 API(예: Yahoo)의 일시 오류가 한 종목 때문에 전체 수집을 막지 않도록 격리한다.
                log.warn("시세 수집 실패 {}: {}", stock.getCode(), e.getMessage());
            }
        }
        if (published > 0) {
            log.debug("시세 {}종목 발행", published);
        }
    }
}
