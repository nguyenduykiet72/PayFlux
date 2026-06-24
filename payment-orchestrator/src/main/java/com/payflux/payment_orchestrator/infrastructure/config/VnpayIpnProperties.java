package com.payflux.payment_orchestrator.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payflux.vnpay")
public record VnpayIpnProperties(
        @NotBlank String hashSecret
) {
}
