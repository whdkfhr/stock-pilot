package com.arok2.stockpilot.dto;

import com.arok2.stockpilot.domain.Stock;

/**
 * 종목 상세. 요약 정보에 투자지표(PER/PBR/ROE/배당률)를 더한다.
 * 이 지표들은 추천 스코어링의 입력이기도 하다.
 * change/changePercent는 전일 종가 대비 등락(수집 전이면 null).
 */
public record StockDetailResponse(
        Long id,
        String code,
        String name,
        String market,
        String currency,
        Long price,
        Long change,
        Double changePercent,
        long watchCount,
        long likeCount,
        double per,
        double pbr,
        double roe,
        double dividendYield
) {
    public static StockDetailResponse of(Stock stock, Long price, Long previousClose) {
        long watch = stock.getWatchCount() == null ? 0L : stock.getWatchCount();
        Long change = null;
        Double changePercent = null;
        if (price != null && previousClose != null && previousClose != 0) {
            change = price - previousClose;
            changePercent = Math.round((change * 10000.0) / previousClose) / 100.0;
        }
        return new StockDetailResponse(
                stock.getId(), stock.getCode(), stock.getName(),
                stock.getMarket().name(), stock.getMarket().currency(),
                price, change, changePercent, watch, stock.getLikeCount(),
                stock.getPer(), stock.getPbr(), stock.getRoe(), stock.getDividendYield());
    }
}
