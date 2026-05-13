package com.payflux.errorcode;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    COMMON_BAD_REQUEST("COMMON_400", "Bad request", HttpStatus.BAD_REQUEST),
    COMMON_UNAUTHORIZED("COMMON_401", "Unauthorized", HttpStatus.UNAUTHORIZED),
    COMMON_FORBIDDEN("COMMON_403", "Forbidden", HttpStatus.FORBIDDEN),
    COMMON_NOT_FOUND("COMMON_404", "Resource not found", HttpStatus.NOT_FOUND),
    COMMON_CONFLICT("COMMON_409", "Conflict", HttpStatus.CONFLICT),
    COMMON_VALIDATION("COMMON_422", "Validation failed", HttpStatus.UNPROCESSABLE_CONTENT),
    COMMON_INTERNAL_ERROR("COMMON_500", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMON_SERVICE_UNAVAILABLE("COMMON_503", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
