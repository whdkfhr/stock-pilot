package com.arok2.stockpilot.watchlist.dto;

import java.time.Instant;

public record WatchlistCreateResponse(
        Long watchlistId,
        Long stockId,
        Long userId,
        Instant createdAt
) {
}
