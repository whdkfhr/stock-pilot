package com.arok2.stockpilot.stock.dto;

import com.arok2.stockpilot.price.domain.PriceChange;
import com.arok2.stockpilot.stock.service.result.StockSummary;

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

    /** 조회 결과를 API 표현으로 평탄화한다(시장 enum → 문자열, 등락 값 객체 → nullable 필드). */
    public static StockSummaryResponse from(StockSummary summary) {
        return new StockSummaryResponse(
                summary.id(),
                summary.code(),
                summary.name(),
                summary.market().name(),
                summary.market().currency(),
                summary.price(),
                PriceChange.changeOrNull(summary.change()),
                PriceChange.percentOrNull(summary.change()),
                summary.watchCount(),
                summary.likeCount()
        );
    }
}
