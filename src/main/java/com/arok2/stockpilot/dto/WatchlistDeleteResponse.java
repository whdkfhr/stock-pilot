package com.arok2.stockpilot.dto;

import java.time.Instant;

public record WatchlistDeleteResponse(
        Long stockId,
        Instant unwatchedAt
) {
}
