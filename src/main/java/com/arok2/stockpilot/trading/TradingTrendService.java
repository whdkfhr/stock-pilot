package com.arok2.stockpilot.trading;

import com.arok2.stockpilot.domain.Stock;
import com.arok2.stockpilot.exception.StockNotFoundException;
import com.arok2.stockpilot.repository.StockRepository;
import com.arok2.stockpilot.trading.TradingTrendResponse.InvestorFlow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * 투자자별 매매동향 제공. 개인/외국인/기관 수급은 KRX/KIS 전용 데이터라 현재는
 * 종목코드+날짜로 결정되는 합성(샘플) 값을 반환한다(하루 동안 안정적). KIS 연동 시 실데이터로 교체.
 * 국내 시장(KOSPI/KOSDAQ) 종목만 대상이며, 그 외는 sample=false·빈 목록.
 */
@Service
public class TradingTrendService {

    private static final String UNIT = "억원";

    private final StockRepository stockRepository;

    public TradingTrendService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public TradingTrendResponse getTrend(String code) {
        Stock stock = stockRepository.findByCode(code)
                .orElseThrow(() -> new StockNotFoundException(code));

        if (!stock.getMarket().isDomestic()) {
            return new TradingTrendResponse(code, UNIT, false, List.of());
        }

        // 종목코드+날짜 시드 → 하루 동안 안정적인 합성 수급. 순매수 합은 대략 0(시장 균형).
        long seed = (code.hashCode() & 0xffffffffL) ^ LocalDate.now().toEpochDay();
        Random r = new Random(seed);
        long foreign = r.nextInt(-400, 401);
        long institution = r.nextInt(-300, 301);
        long individual = -(foreign + institution); // 나머지를 개인이 흡수

        return new TradingTrendResponse(code, UNIT, true, List.of(
                new InvestorFlow("개인", individual),
                new InvestorFlow("외국인", foreign),
                new InvestorFlow("기관", institution)
        ));
    }
}
