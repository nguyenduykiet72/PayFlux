package com.payflux.exception;

import com.payflux.errorcode.ErrorCode;

import java.util.Map;

public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    protected BaseException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> metadata) {
        super(message != null ? message : errorCode.defaultMessage(), cause);
        this.errorCode = errorCode;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
