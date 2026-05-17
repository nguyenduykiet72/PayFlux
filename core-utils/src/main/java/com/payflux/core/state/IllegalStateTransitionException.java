package com.payflux.core.state;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;

import java.util.Map;

public class IllegalStateTransitionException extends BusinessException {
    public IllegalStateTransitionException(PaymentState from, PaymentState to) {
        super(CommonErrorCode.COMMON_CONFLICT,
              "Illegal payment state transition: " + from + " → " + to,
              null,
              Map.of("from", from.name(), "to", to.name()));
    }
}
