package com.arok2.stockpilot.price.dto;

import com.arok2.stockpilot.price.domain.PriceHistory;

import java.time.Instant;

public record PriceHistoryResponse(
        String code,
        long price,
        long volume,
        Instant tradedAt
) {
    public static PriceHistoryResponse from(PriceHistory history) {
        return new PriceHistoryResponse(
                history.getCode(), history.getPrice(), history.getVolume(), history.getTradedAt());
    }
}
