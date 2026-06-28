package com.payflux.webhook_service.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Local dev endpoint — simulates merchant webhook receiver.
 * Default target URL in application.yaml points here.
 */
@RestController
public class DevWebhookController {

  private static final Logger log = LoggerFactory.getLogger(DevWebhookController.class);

  @PostMapping("/dev/webhook")
  public Map<String, String> receive(
      @RequestBody String body,
      @RequestHeader(value = "X-PayFlux-Signature", required = false) String signature) {
    log.info("Dev webhook received signature={} body={}", signature, body);
    return Map.of("status", "ok");
  }
}
