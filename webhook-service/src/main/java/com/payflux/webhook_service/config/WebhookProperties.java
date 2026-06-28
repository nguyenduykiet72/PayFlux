package com.payflux.webhook_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.UUID;

@ConfigurationProperties(prefix = "payflux.webhook")
public record WebhookProperties(
        java.time.Duration idempotencyTtl,
        Map<UUID, MerchantWebhook> merchants
) {
    public WebhookProperties {
        idempotencyTtl = idempotencyTtl == null ? java.time.Duration.ofDays(7) : idempotencyTtl;
        merchants = merchants == null ? Map.of() : Map.copyOf(merchants);
    }

    public record MerchantWebhook(String url, String secret) {}
}
