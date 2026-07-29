package com.arok2.stockpilot.stock.service.result;

import com.arok2.stockpilot.stock.domain.Stock;

/**
 * 종목 상세 조회 결과. 요약({@link StockSummary})에 투자지표를 더한다.
 * 이 지표들은 추천 스코어링의 입력이기도 하다.
 */
public record StockDetail(
        StockSummary summary,
        double per,
        double pbr,
        double roe,
        double dividendYield
) {

    public static StockDetail of(Stock stock, Long price, Long previousClose) {
        return new StockDetail(
                StockSummary.of(stock, price, previousClose),
                stock.getPer(),
                stock.getPbr(),
                stock.getRoe(),
                stock.getDividendYield()
        );
    }
}
