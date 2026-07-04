package com.arok2.stockpilot.exception;

import com.arok2.stockpilot.dto.error.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

        ErrorResponse body = ErrorResponse.of(
                "VALIDATION_ERROR",
                "요청 본문 형식이 올바르지 않습니다"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        ErrorResponse body = ErrorResponse.of("DUPLICATE_EMAIL", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * AuthService에서 유니크 제약 위반을 DuplicateEmailException으로 변환하므로
     * 정상 흐름에서는 이 핸들러까지 도달하지 않는다.
     * 다만 다른 경로(예: 향후 추가되는 배치/마이그레이션 로직 등)에서 발생하는
     * 무결성 위반이 500으로 노출되는 것을 방지하기 위한 방어적 처리이다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Unhandled data integrity violation: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of("DUPLICATE_EMAIL", "이미 사용 중인 값입니다");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse body = ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
