package com.payflux.errorcode;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();

    String defaultMessage();

    HttpStatus httpStatus();
}