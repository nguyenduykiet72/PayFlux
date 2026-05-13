package com.payflux.response;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.slf4j.MDC;

public record ApiResponse<T>(
        int status,
        T data,
        String message,
        Instant timestamp,
        String traceId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                data,
                HttpStatus.OK.getReasonPhrase(),
                Instant.now(),
                getTraceId());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                data, message,
                Instant.now(),
                getTraceId());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null, message,
                Instant.now(),
                getTraceId());
    }

    private static String getTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }
}
