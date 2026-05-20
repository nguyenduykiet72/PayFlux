package com.payflux.payment_orchestrator.presentation;

import com.payflux.core.state.PaymentState;
import com.payflux.payment_orchestrator.application.PaymentService;
import com.payflux.payment_orchestrator.domain.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payments")
public class InternalPaymentController {
    private final PaymentService paymentService;

    public record CreatePaymentRequest(
            @NotBlank String idempotencyKey,
            @Positive long amountMinor,
            @NotBlank String currency,
            @NotBlank String provider
    ) {
    }

    public record PaymentResponse(
            UUID paymentId, UUID merchantId, String status,
            long amountMinor, String currency, int version
    ) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPending(
                request.idempotencyKey(), request.amountMinor(), request.currency(), request.provider()
        );

        return toResponse(payment);
    }

    @PostMapping("/{id}/transition/{status}")
    public PaymentResponse transitionPayment(@PathVariable UUID id, @PathVariable PaymentState status) {
        return toResponse(paymentService.transitionTo(id, status));
    }

    private static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.paymentId(), payment.merchantId(), payment.status().name(),
                payment.amountMinor(), payment.currency(), payment.version());
    }
}
