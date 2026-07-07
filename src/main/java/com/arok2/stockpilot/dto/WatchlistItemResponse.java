package com.arok2.stockpilot.dto;

import java.time.Instant;

public record WatchlistItemResponse(
        Long watchlistId,
        Long stockId,
        String stockCode,
        String stockName,
        Long watchCount,
        Instant createdAt
) {
}
