package com.raghad.aichat.exception;

import com.raghad.aichat.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_JSON", "Request body must contain valid JSON."));
    }

    @ExceptionHandler(UpstreamApiException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(UpstreamApiException ex) {
        HttpStatus status = switch (ex.getUpstreamStatus() != null ? ex.getUpstreamStatus().value() : 0) {
            case 401, 403 -> HttpStatus.BAD_GATEWAY;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 0 -> ex.isTimeout() ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status)
                .body(ErrorResponse.of("UPSTREAM_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled request failure", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR", "Something went wrong. Please try again."));
    }
}
