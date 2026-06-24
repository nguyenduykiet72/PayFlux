package com.payflux.vnpay_adapter.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "payflux.vnpay")
public record VnpayProperties(
        @NotBlank String tmnCode,
        @NotBlank String hashSecret,
        @NotBlank String payUrl,
        @NotBlank String apiUrl,
        @NotBlank String returnUrl,
        @NotBlank String version,
        @NotBlank String command,
        @NotNull Duration paymentExpiry
) {
}
