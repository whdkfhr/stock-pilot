package com.arok2.stockpilot.dto;

import java.time.Instant;

public record WatchlistCreateResponse(
        Long watchlistId,
        Long stockId,
        Long userId,
        Instant createdAt
) {
}
