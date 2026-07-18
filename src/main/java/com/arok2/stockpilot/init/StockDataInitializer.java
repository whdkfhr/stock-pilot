package com.arok2.stockpilot.init;

import com.arok2.stockpilot.domain.MarketType;
import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.repository.StockRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 데모용 종목 시드. 애플리케이션 시작 시 정의된 종목이 없으면 삽입한다(코드 기준 upsert, 멱등).
 * 재무지표는 추천 스코어링 입력이자 시연용 값이다. 시장(KOSPI/KOSDAQ/NASDAQ)에 따라
 * Yahoo 심볼 접미사·통화가 결정된다. {@code stockpilot.seed.enabled=false}면 비활성(테스트 등).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "stockpilot.seed.enabled", havingValue = "true", matchIfMissing = true)
public class StockDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockDataInitializer.class);

    private final StockRepository stockRepository;

    public StockDataInitializer(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    private record Seed(String code, String name, MarketType market,
                        double per, double pbr, double roe, double dividendYield) {
    }

    private static final List<Seed> SEEDS = List.of(
            // KOSPI
            new Seed("005930", "삼성전자", MarketType.KOSPI, 12, 1.2, 15, 2.5),
            new Seed("000660", "SK하이닉스", MarketType.KOSPI, 13, 1.5, 22, 1.0),
            new Seed("373220", "LG에너지솔루션", MarketType.KOSPI, 60, 3.5, 6, 0.2),
            new Seed("207940", "삼성바이오로직스", MarketType.KOSPI, 55, 5.5, 12, 0.0),
            new Seed("005380", "현대차", MarketType.KOSPI, 5, 0.6, 12, 4.5),
            new Seed("000270", "기아", MarketType.KOSPI, 4, 0.8, 18, 5.0),
            new Seed("005490", "POSCO홀딩스", MarketType.KOSPI, 9, 0.5, 6, 3.5),
            new Seed("035420", "NAVER", MarketType.KOSPI, 20, 1.3, 8, 0.5),
            new Seed("035720", "카카오", MarketType.KOSPI, 40, 1.8, 4, 0.1),
            new Seed("105560", "KB금융", MarketType.KOSPI, 6, 0.5, 9, 5.5),
            new Seed("055550", "신한지주", MarketType.KOSPI, 6, 0.5, 9, 5.0),
            new Seed("033780", "KT&G", MarketType.KOSPI, 10, 1.3, 12, 6.0),
            // KOSDAQ
            new Seed("247540", "에코프로비엠", MarketType.KOSDAQ, 45, 4.0, 10, 0.1),
            new Seed("086520", "에코프로", MarketType.KOSDAQ, 30, 5.0, 15, 0.2),
            new Seed("196170", "알테오젠", MarketType.KOSDAQ, 90, 12.0, 14, 0.0),
            // 미국(NASDAQ)
            new Seed("AAPL", "애플", MarketType.NASDAQ, 30, 8.0, 30, 0.5),
            new Seed("NVDA", "엔비디아", MarketType.NASDAQ, 45, 20.0, 35, 0.03),
            new Seed("MSFT", "마이크로소프트", MarketType.NASDAQ, 33, 12.0, 40, 0.8),
            new Seed("TSLA", "테슬라", MarketType.NASDAQ, 70, 10.0, 15, 0.0),
            new Seed("AMZN", "아마존", MarketType.NASDAQ, 40, 7.0, 20, 0.0)
    );

    @Override
    @Transactional
    public void run(String... args) {
        int inserted = 0;
        for (Seed s : SEEDS) {
            var existing = stockRepository.findByCode(s.code());
            if (existing.isEmpty()) {
                stockRepository.save(Stock.of(s.code(), s.name(), s.market(),
                        s.per(), s.pbr(), s.roe(), s.dividendYield()));
                inserted++;
            } else {
                // 기존 종목의 시장을 보정(멱등 backfill). 값이 같으면 변경 감지가 UPDATE를 생략한다.
                existing.get().updateMarket(s.market());
            }
        }
        if (inserted > 0) {
            log.info("종목 시드: {}건 삽입 (총 {}종목)", inserted, stockRepository.count());
        }
    }
}
