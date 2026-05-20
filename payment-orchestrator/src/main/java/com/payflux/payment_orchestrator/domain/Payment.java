package com.payflux.payment_orchestrator.domain;

import com.payflux.core.state.PaymentState;

import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID paymentId,
        UUID merchantId,
        String idempotencyKey,
        long amountMinor,
        String currency,
        String provider,
        PaymentState status,
        int version,
        Instant createdAt,
        Instant updatedAt) {
}