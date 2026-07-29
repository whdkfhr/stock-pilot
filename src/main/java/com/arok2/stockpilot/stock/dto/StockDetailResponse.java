package com.arok2.stockpilot.stock.dto;

import com.arok2.stockpilot.stock.service.result.StockDetail;

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

    /** 상세 조회 결과를 API 표현으로 평탄화한다(요약 평탄화는 {@link StockSummaryResponse}와 동일 규칙). */
    public static StockDetailResponse from(StockDetail detail) {
        StockSummaryResponse summary = StockSummaryResponse.from(detail.summary());
        return new StockDetailResponse(
                summary.id(),
                summary.code(),
                summary.name(),
                summary.market(),
                summary.currency(),
                summary.price(),
                summary.change(),
                summary.changePercent(),
                summary.watchCount(),
                summary.likeCount(),
                detail.per(),
                detail.pbr(),
                detail.roe(),
                detail.dividendYield()
        );
    }
}
