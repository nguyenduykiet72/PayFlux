package com.payflux.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.payflux.errorcode.CommonErrorCode;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_TYPE_PREFIX = "https://payflux.dev/errors/";

    @ExceptionHandler(BaseException.class)
    public ProblemDetail handleBaseException(BaseException ex, HttpServletRequest request) {
        var code = ex.getErrorCode();
        logger.warn("BaseException [{}]: {}", code.code(), ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.httpStatus(), ex.getMessage());
        pd.setTitle(code.code());
        pd.setType(URI.create(ERROR_TYPE_PREFIX + code.code()));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("traceId", MDC.get("traceId"));
        if (!ex.getMetadata().isEmpty()) {
            pd.setProperty("metadata", ex.getMetadata());
        }
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError).toList();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CommonErrorCode.COMMON_VALIDATION.httpStatus(),
                "Validation failed");

        problemDetail.setTitle(CommonErrorCode.COMMON_VALIDATION.code());
        problemDetail.setType(URI.create(ERROR_TYPE_PREFIX + CommonErrorCode.COMMON_VALIDATION.code()));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", MDC.get("traceId"));
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(Throwable.class)
    public ProblemDetail handleAny(Throwable ex, HttpServletRequest request) {
        logger.error("Unexpected error", ex);
        var code = CommonErrorCode.COMMON_INTERNAL_ERROR;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(code.httpStatus(), code.defaultMessage());
        problemDetail.setTitle(code.code());
        problemDetail.setType(URI.create(ERROR_TYPE_PREFIX + code.code()));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("traceId", MDC.get("traceId"));
        return problemDetail;
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
