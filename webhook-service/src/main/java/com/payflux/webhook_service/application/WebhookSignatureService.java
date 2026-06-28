package com.payflux.webhook_service.application;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class WebhookSignatureService {

  private static final String ALGORITHM = "HmacSHA256";

  public String signSha256Hex(String secret, String body) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
      return "sha256=" + HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Webhook HMAC signing failed", e);
    }
  }
}
