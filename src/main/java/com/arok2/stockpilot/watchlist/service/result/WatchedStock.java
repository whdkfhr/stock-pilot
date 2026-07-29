package com.arok2.stockpilot.watchlist.service.result;

import java.time.Instant;

/**
 * 관심종목 조회 결과(관심등록 + 종목 정보 조합). API 응답(WatchlistItemResponse)과 분리해
 * 서비스가 HTTP 계약에 묶이지 않도록 한다.
 */
public record WatchedStock(
        Long watchlistId,
        Long stockId,
        String stockCode,
        String stockName,
        Long watchCount,
        Instant createdAt
) {
}
