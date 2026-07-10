package com.arok2.stockpilot.notification.dto;

import com.arok2.stockpilot.notification.domain.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String stockCode,
        String message,
        long price,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getStockCode(),
                notification.getMessage(),
                notification.getPrice(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
