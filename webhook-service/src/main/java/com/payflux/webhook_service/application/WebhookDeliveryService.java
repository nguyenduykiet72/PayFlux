package com.payflux.webhook_service.application;

import com.payflux.webhook_service.config.WebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WebhookDeliveryService {

  private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
  private static final String SIGNATURE_HEADER = "X-PayFlux-Signature";

  private final RestClient restClient;
  private final WebhookSignatureService signatureService;

  public WebhookDeliveryService(WebhookSignatureService signatureService) {
    this.signatureService = signatureService;
    this.restClient = RestClient.builder().build();
  }

  public void deliver(WebhookProperties.MerchantWebhook target, String payloadJson) {
    String signature = signatureService.signSha256Hex(target.secret(), payloadJson);
    log.info("Delivering webhook to {}", target.url());
    restClient.post()
        .uri(target.url())
        .contentType(MediaType.APPLICATION_JSON)
        .header(SIGNATURE_HEADER, signature)
        .body(payloadJson)
        .retrieve()
        .toBodilessEntity();
  }
}
