package com.arok2.stockpilot.stock.service.result;

import com.arok2.stockpilot.price.domain.PriceChange;
import com.arok2.stockpilot.stock.domain.MarketType;
import com.arok2.stockpilot.stock.domain.Stock;

/**
 * 종목 요약 조회 결과(종목 마스터 + 최신가). 표현 형식이 아니라 <b>도메인 타입</b>으로 들고 있다
 * — 시장은 {@link MarketType}, 등락은 {@link PriceChange}. 문자열 평탄화·null 분해 같은
 * 표현 관심사는 응답 DTO가 담당한다.
 *
 * @param price  최신가. 아직 수집 전이면 {@code null}
 * @param change 전일 대비 등락. 계산할 수 없으면 {@code null}
 */
public record StockSummary(
        Long id,
        String code,
        String name,
        MarketType market,
        Long price,
        PriceChange change,
        long watchCount,
        long likeCount
) {

    public static StockSummary of(Stock stock, Long price, Long previousClose) {
        return new StockSummary(
                stock.getId(),
                stock.getCode(),
                stock.getName(),
                stock.getMarket(),
                price,
                PriceChange.of(price, previousClose),
                stock.getWatchCount() == null ? 0L : stock.getWatchCount(),
                stock.getLikeCount()
        );
    }
}
