package com.payflux.core.tenant;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;

import java.util.Map;

public class MissingTenantContextException extends BusinessException {
    public MissingTenantContextException() {
        super(CommonErrorCode.COMMON_UNAUTHORIZED,
              "Request missing tenant context (X-Merchant-Id header or gRPC x-merchant-id metadata)",
              null,
              Map.of());
    }
}
