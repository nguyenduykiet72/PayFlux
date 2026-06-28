package com.payflux.payment_orchestrator.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflux.core.state.PaymentState;
import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.InfrastructureException;
import com.payflux.payment_orchestrator.domain.OutboxEvent;
import com.payflux.payment_orchestrator.domain.Payment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxEventFactory {

    public static final String PAYMENT_CAPTURED = "payment.captured";

    private final ObjectMapper objectMapper;

    public OutboxEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent paymentCaptured(Payment payment) {
        UUID eventId = UUID.randomUUID();
        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                    "event_id", eventId.toString(),
                    "payment_id", payment.paymentId().toString(),
                    "merchant_id", payment.merchantId().toString(),
                    "status", PaymentState.CAPTURED.name(),
                    "amount_minor", payment.amountMinor(),
                    "currency", payment.currency(),
                    "provider", payment.provider(),
                    "occurred_at", Instant.now().toString()
            ));
            return new OutboxEvent(
                    eventId,
                    payment.paymentId(),
                    PAYMENT_CAPTURED,
                    payloadJson,
                    Instant.now()
            );
        } catch (JsonProcessingException e) {
            throw new InfrastructureException(
                    CommonErrorCode.COMMON_INTERNAL_ERROR,
                    "Failed to serialize outbox payload",
                    e,
                    Map.of("paymentId", payment.paymentId().toString()));
        }
    }
}
