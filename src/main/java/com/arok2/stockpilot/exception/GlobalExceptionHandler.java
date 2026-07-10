package com.arok2.stockpilot.exception;

import com.arok2.stockpilot.dto.error.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();

        ErrorResponse body = ErrorResponse.of(
                "VALIDATION_ERROR",
                "입력값이 올바르지 않습니다",
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Malformed request body: {}", ex.getMessage());

        List<ErrorResponse.FieldError> details = extractFieldDetails(ex);

        ErrorResponse body = ErrorResponse.of(
                "VALIDATION_ERROR",
                "요청 본문 형식이 올바르지 않습니다",
                details.isEmpty() ? null : details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Enum 필드(riskProfile, investmentPeriod 등)에 정의되지 않은 값이 들어오면
     * Jackson이 InvalidFormatException을 던지며 이는 HttpMessageNotReadableException으로
     * 래핑되어 전달된다. 이 경우 어떤 필드가 잘못되었는지 클라이언트가 식별할 수 있도록
     * 필드 경로를 details에 채운다.
     */
    private List<ErrorResponse.FieldError> extractFieldDetails(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException
                && !invalidFormatException.getPath().isEmpty()) {
            String fieldName = invalidFormatException.getPath()
                    .get(invalidFormatException.getPath().size() - 1)
                    .getFieldName();
            String reason = "허용되지 않는 값입니다: " + invalidFormatException.getValue();
            return List.of(new ErrorResponse.FieldError(fieldName, reason));
        }
        return List.of();
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        ErrorResponse body = ErrorResponse.of("DUPLICATE_EMAIL", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse body = ErrorResponse.of("INVALID_CREDENTIALS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of("USER_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFound(StockNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of("STOCK_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(WatchlistNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWatchlistNotFound(WatchlistNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of("WATCHLIST_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(WatchlistAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleWatchlistAlreadyExists(WatchlistAlreadyExistsException ex) {
        ErrorResponse body = ErrorResponse.of("WATCHLIST_ALREADY_EXISTS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAlertNotFound(AlertNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of("ALERT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationNotFound(NotificationNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of("NOTIFICATION_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        ErrorResponse body = ErrorResponse.of("UNAUTHORIZED", "인증이 필요합니다");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * AuthService/WatchlistService에서 유니크 제약 위반을 각각의 전용 예외로 변환하므로
     * 정상 흐름에서는 이 핸들러까지 도달하지 않는다.
     * 다만 다른 경로(예: 향후 추가되는 배치/마이그레이션 로직 등)에서 발생하는
     * 무결성 위반이 500으로 노출되는 것을 방지하기 위한 방어적 처리이다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Unhandled data integrity violation: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of("DUPLICATE_ENTRY", "이미 사용 중인 값입니다");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse body = ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
