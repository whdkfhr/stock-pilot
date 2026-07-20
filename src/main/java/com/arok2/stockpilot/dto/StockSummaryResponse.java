package com.arok2.stockpilot.dto;

import com.arok2.stockpilot.domain.Stock;

/**
 * 종목 목록 요약. 관심종목 등록은 stockId(Long), 시세·좋아요·조회는 code를 쓰므로 둘 다 내려준다.
 * price는 최신가 캐시에서 온 값이며, 아직 수집 전이면 null이다.
 * change/changePercent는 전일 종가 대비 등락(수집 전이면 null).
 */
public record StockSummaryResponse(
        Long id,
        String code,
        String name,
        String market,
        String currency,
        Long price,
        Long change,
        Double changePercent,
        long watchCount,
        long likeCount
) {
    public static StockSummaryResponse of(Stock stock, Long price, Long previousClose) {
        long watch = stock.getWatchCount() == null ? 0L : stock.getWatchCount();
        Long change = null;
        Double changePercent = null;
        if (price != null && previousClose != null && previousClose != 0) {
            change = price - previousClose;
            changePercent = Math.round((change * 10000.0) / previousClose) / 100.0;
        }
        return new StockSummaryResponse(
                stock.getId(), stock.getCode(), stock.getName(),
                stock.getMarket().name(), stock.getMarket().currency(),
                price, change, changePercent, watch, stock.getLikeCount());
    }
}
