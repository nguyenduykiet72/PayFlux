package com.payflux.payment_orchestrator.application;

import com.payflux.core.state.PaymentState;
import com.payflux.core.state.StateTransitionManager;
import com.payflux.core.tenant.TenantContext;
import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;
import com.payflux.payment_orchestrator.domain.Payment;
import com.payflux.payment_orchestrator.infrastructure.persistence.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentMapper mapper;
    private final IdempotencyService idempotencyService;

    @Transactional
    public Payment createPending(String idempotencyKey, long amountMinor, String currency, String provider) {
        UUID merchantId = TenantContext.get();
        UUID paymentId = UUID.randomUUID();

        boolean acquired = idempotencyService.tryAcquire(merchantId, idempotencyKey, paymentId);
        if (!acquired) {
            return mapper.findByIdempotencyKey(merchantId, idempotencyKey)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_CONFLICT,
                            "Idempotency key already in use"));
        }

        Payment payment = new Payment(
                paymentId, merchantId, idempotencyKey, amountMinor, currency, provider,
                PaymentState.PENDING, 0, Instant.now(), Instant.now()
        );
        mapper.insert(payment);
        return payment;
    }

    @Transactional
    public Payment transitionTo(UUID paymentId, PaymentState newState) {
        Payment current = mapper.findById(paymentId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.COMMON_NOT_FOUND,
                        "Payment not found", null, Map.of("paymentId", paymentId.toString())));

        StateTransitionManager.assertCanTransition(current.status(), newState);

        int updated = mapper.updateStatus(paymentId, newState, current.version());
        if (updated != 1) {
            throw new BusinessException(CommonErrorCode.COMMON_CONFLICT,
                    "Payment state transition failed", null, Map.of(
                    "paymentId", paymentId.toString(),
                    "expectedVersion", String.valueOf(current.version())));
        }

        return mapper.findById(paymentId).orElseThrow();
    }
}
