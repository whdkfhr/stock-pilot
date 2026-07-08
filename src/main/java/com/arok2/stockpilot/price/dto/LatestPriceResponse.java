package com.arok2.stockpilot.price.dto;

public record LatestPriceResponse(
        String code,
        long price
) {
}
