package com.payflux.exception;

import com.payflux.errorcode.ErrorCode;

import java.util.Map;

public class BusinessException extends BaseException{
    public BusinessException(ErrorCode errorCode) { this(errorCode, null, null, null); }
    public BusinessException(ErrorCode errorCode, String message) { this(errorCode, message, null, null); }
    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> metadata) { this(errorCode, message, null, metadata); }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> metadata) {
        super(errorCode, message, cause, metadata);
    }
}
