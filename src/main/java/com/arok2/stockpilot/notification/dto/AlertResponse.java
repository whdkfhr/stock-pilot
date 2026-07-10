package com.arok2.stockpilot.notification.dto;

import com.arok2.stockpilot.notification.domain.AlertCondition;
import com.arok2.stockpilot.notification.domain.AlertDirection;
import com.arok2.stockpilot.notification.domain.AlertStatus;

import java.time.Instant;

public record AlertResponse(
        Long id,
        String stockCode,
        AlertDirection direction,
        long threshold,
        AlertStatus status,
        Instant createdAt,
        Instant triggeredAt
) {
    public static AlertResponse from(AlertCondition alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getStockCode(),
                alert.getDirection(),
                alert.getThreshold(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getTriggeredAt());
    }
}
