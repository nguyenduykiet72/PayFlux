package com.payflux.payment_orchestrator.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payloadJson,
        Instant createdAt
) {
}
