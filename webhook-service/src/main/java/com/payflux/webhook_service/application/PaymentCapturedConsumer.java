package com.payflux.webhook_service.application;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.payflux.webhook_service.config.KafkaConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentCapturedConsumer {

  private static final Logger log = LoggerFactory.getLogger(PaymentCapturedConsumer.class);

  private final ObjectMapper objectMapper;
  private final MerchantWebhookResolver webhookResolver;
  private final WebhookIdempotencyService idempotencyService;
  private final WebhookDeliveryService deliveryService;

  public PaymentCapturedConsumer(
      ObjectMapper objectMapper,
      MerchantWebhookResolver webhookResolver,
      WebhookIdempotencyService idempotencyService,
      WebhookDeliveryService deliveryService) {
    this.objectMapper = objectMapper;
    this.webhookResolver = webhookResolver;
    this.idempotencyService = idempotencyService;
    this.deliveryService = deliveryService;
  }

  @KafkaListener(
      topics = KafkaConsumerConfig.PAYMENT_CAPTURED_TOPIC,
      containerFactory = "kafkaListenerContainerFactory")
  public void onPaymentCaptured(String payloadJson) {
    log.info("Received payment.captured event");
    try {
      JsonNode root = objectMapper.readTree(payloadJson);
      String eventId = textOrThrow(root, "event_id");
      String merchantIdRaw = textOrThrow(root, "merchant_id");

      if (idempotencyService.isDuplicate(eventId)) {
        log.info("Skipping duplicate webhook eventId={}", eventId);
        return;
      }

      var merchantId = UUID.fromString(merchantIdRaw);
      var target = webhookResolver.resolve(merchantId);
      deliveryService.deliver(target, payloadJson);
      idempotencyService.markProcessed(eventId);
      log.info("Webhook delivered eventId={} merchantId={}", eventId, merchantId);
    } catch (Exception e) {
      log.error("Failed to process payment.captured payload={}", payloadJson, e);
      throw new IllegalStateException("Webhook processing failed", e);
    }
  }

  private static String textOrThrow(JsonNode root, String field) {
    JsonNode node = root.get(field);
    if (node == null || node.isNull() || node.asText().isBlank()) {
      throw new IllegalArgumentException("Missing field: " + field);
    }
    return node.asText();
  }
}
