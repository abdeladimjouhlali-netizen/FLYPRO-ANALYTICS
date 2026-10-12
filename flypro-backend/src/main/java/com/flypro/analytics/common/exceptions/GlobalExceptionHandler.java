package com.flypro.analytics.common.exceptions;

import com.flypro.analytics.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Gestion centralisée des exceptions HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("API error code={} path={} message={}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        } else {
            log.warn("API error code={} path={} message={}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            AuthenticationException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        String message = (ex instanceof BadCredentialsException)
                ? "Invalid username or password"
                : ex.getMessage();
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, request.getRequestURI(), null);
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleAccountStatus(AuthenticationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", ex.getMessage(),
                request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to access this resource",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error path={}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request.getRequestURI(), null);
    }

    private ErrorResponse.FieldErrorDetail toFieldError(FieldError error) {
        return new ErrorResponse.FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String code,
                                                String message,
                                                String path,
                                                List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
