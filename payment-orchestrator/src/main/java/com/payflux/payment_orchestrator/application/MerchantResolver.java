package com.payflux.payment_orchestrator.application;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;
import com.payflux.payment_orchestrator.infrastructure.config.MerchantMappingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantResolver {
    private final MerchantMappingProperties merchantMappingProperties;

    public UUID resolveFromConsumerUserName(String consumerUserName) {
        if (consumerUserName == null || consumerUserName.isBlank()) {
            throw new BusinessException(CommonErrorCode.COMMON_UNAUTHORIZED,
                    "Missing consumer identity", null, Map.of());
        }
        UUID merchantId = merchantMappingProperties.byConsumerUserName().get(consumerUserName);
        if (merchantId == null) {
            throw new BusinessException(CommonErrorCode.COMMON_UNAUTHORIZED,
                    "Unknown consumer identity", null,
                    Map.of("consumerUserName", consumerUserName));
        }
        return merchantId;
    }
}
