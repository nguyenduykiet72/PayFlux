package com.payflux.payment_orchestrator.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.UUID;

@ConfigurationProperties(prefix = "payflux.merchants")
public record MerchantMappingProperties(
        Map<String, UUID> byConsumerUserName
) {
    public MerchantMappingProperties {
        byConsumerUserName = byConsumerUserName == null ? Map.of() : Map.copyOf(byConsumerUserName);
    }
}
