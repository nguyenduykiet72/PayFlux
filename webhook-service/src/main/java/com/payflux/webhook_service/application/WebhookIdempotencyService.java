package com.payflux.webhook_service.application;

import com.payflux.webhook_service.config.WebhookProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class WebhookIdempotencyService {

  private static final String KEY_PREFIX = "webhook:processed:";

  private final StringRedisTemplate redis;
  private final Duration ttl;

  public WebhookIdempotencyService(StringRedisTemplate redis, WebhookProperties webhookProperties) {
    this.redis = redis;
    this.ttl = webhookProperties.idempotencyTtl();
  }

  public boolean isDuplicate(String eventId) {
    return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + eventId));
  }

  public void markProcessed(String eventId) {
    redis.opsForValue().set(KEY_PREFIX + eventId, "1", ttl);
  }
}
