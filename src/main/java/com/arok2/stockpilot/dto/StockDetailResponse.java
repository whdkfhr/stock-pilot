package com.arok2.stockpilot.dto;

import com.arok2.stockpilot.domain.Stock;

/**
 * 종목 상세. 요약 정보에 투자지표(PER/PBR/ROE/배당률)를 더한다.
 * 이 지표들은 추천 스코어링의 입력이기도 하다.
 */
public record StockDetailResponse(
        Long id,
        String code,
        String name,
        Long price,
        long watchCount,
        long likeCount,
        double per,
        double pbr,
        double roe,
        double dividendYield
) {
    public static StockDetailResponse of(Stock stock, Long price) {
        long watch = stock.getWatchCount() == null ? 0L : stock.getWatchCount();
        return new StockDetailResponse(
                stock.getId(), stock.getCode(), stock.getName(), price, watch, stock.getLikeCount(),
                stock.getPer(), stock.getPbr(), stock.getRoe(), stock.getDividendYield());
    }
}
