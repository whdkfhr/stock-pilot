```java
package com.arok2.stockpilot.dto.error;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> details
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(code, message, details);
    }

    public record FieldErrorDetail(String field, String reason) {
    }
}
```
