package com.arok2.stockpilot.dto.error;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldError> details
) {
    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, List<FieldError> details) {
        return new ErrorResponse(code, message, details);
    }
}
