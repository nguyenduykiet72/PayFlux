package com.payflux.webhook_service.application;

import com.payflux.webhook_service.config.WebhookProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MerchantWebhookResolver {

  private final WebhookProperties webhookProperties;

  public MerchantWebhookResolver(WebhookProperties webhookProperties) {
    this.webhookProperties = webhookProperties;
  }

  public WebhookProperties.MerchantWebhook resolve(UUID merchantId) {
    WebhookProperties.MerchantWebhook webhook = webhookProperties.merchants().get(merchantId);
    if (webhook == null || webhook.url() == null || webhook.url().isBlank()) {
      throw new IllegalStateException("No webhook configured for merchant " + merchantId);
    }
    return webhook;
  }
}
