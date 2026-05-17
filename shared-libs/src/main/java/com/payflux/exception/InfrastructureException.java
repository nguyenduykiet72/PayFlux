package com.payflux.exception;

import java.util.Map;

import com.payflux.errorcode.ErrorCode;

public class InfrastructureException extends BaseException {
    public InfrastructureException(ErrorCode errorCode) {
        this(errorCode, null, null, null);
    }

    public InfrastructureException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public InfrastructureException(ErrorCode errorCode, String message, Map<String, Object> metadata) {
        this(errorCode, message, null, metadata);
    }

    public InfrastructureException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> metadata) {
        super(errorCode, message, cause, metadata);
    }
}
