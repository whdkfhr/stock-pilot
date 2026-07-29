package com.arok2.stockpilot.watchlist.dto;

import com.arok2.stockpilot.watchlist.domain.Watchlist;

import java.time.Instant;

public record WatchlistCreateResponse(
        Long watchlistId,
        Long stockId,
        Long userId,
        Instant createdAt
) {

    public static WatchlistCreateResponse from(Watchlist watchlist) {
        return new WatchlistCreateResponse(
                watchlist.getId(),
                watchlist.getStockId(),
                watchlist.getUserId(),
                watchlist.getCreatedAt()
        );
    }
}
