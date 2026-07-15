package com.arok2.stockpilot.dto;

import com.arok2.stockpilot.domain.Stock;

/**
 * 종목 목록 요약. 관심종목 등록은 stockId(Long), 시세·좋아요·조회는 code를 쓰므로 둘 다 내려준다.
 * price는 최신가 캐시에서 온 값이며, 아직 수집 전이면 null이다.
 */
public record StockSummaryResponse(
        Long id,
        String code,
        String name,
        Long price,
        long watchCount,
        long likeCount
) {
    public static StockSummaryResponse of(Stock stock, Long price) {
        long watch = stock.getWatchCount() == null ? 0L : stock.getWatchCount();
        return new StockSummaryResponse(
                stock.getId(), stock.getCode(), stock.getName(), price, watch, stock.getLikeCount());
    }
}
