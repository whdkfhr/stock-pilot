package com.arok2.stockpilot.notification.dto;

import com.arok2.stockpilot.notification.domain.AlertDirection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AlertCreateRequest(

        @NotBlank(message = "종목 코드는 필수입니다")
        String stockCode,

        @NotNull(message = "알림 방향은 필수입니다")
        AlertDirection direction,

        @Positive(message = "임계 가격은 0보다 커야 합니다")
        long threshold
) {
}
