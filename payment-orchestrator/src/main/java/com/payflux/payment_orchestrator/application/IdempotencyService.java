package com.payflux.payment_orchestrator.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";
    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(UUID merchantId, String idempotencyKey, UUID paymentId) {
        String key = KEY_PREFIX + merchantId + ":" + idempotencyKey;
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, paymentId.toString(), TTL);
        return Boolean.TRUE.equals(result);
    }
}
