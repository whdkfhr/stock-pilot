package com.arok2.stockpilot.watchlist.dto;

import com.arok2.stockpilot.watchlist.service.result.WatchedStock;

import java.time.Instant;

public record WatchlistItemResponse(
        Long watchlistId,
        Long stockId,
        String stockCode,
        String stockName,
        Long watchCount,
        Instant createdAt
) {

    public static WatchlistItemResponse from(WatchedStock watched) {
        return new WatchlistItemResponse(
                watched.watchlistId(),
                watched.stockId(),
                watched.stockCode(),
                watched.stockName(),
                watched.watchCount(),
                watched.createdAt()
        );
    }
}
