package com.payflux.exception;

import java.util.Map;

import com.payflux.errorcode.ErrorCode;

public class ValidationException extends BaseException {
    public ValidationException(ErrorCode errorCode) {
        this(errorCode, null, null, null);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public ValidationException(ErrorCode errorCode, String message, Map<String, Object> metadata) {
        this(errorCode, message, null, metadata);
    }

    public ValidationException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> metadata) {
        super(errorCode, message, cause, metadata);
    }
}
