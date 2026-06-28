package com.payflux.payment_orchestrator.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;
import com.payflux.payment_orchestrator.application.PaymentInitiationService;
import com.payflux.payment_orchestrator.application.PaymentService;
import com.payflux.payment_orchestrator.domain.Payment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/payments")
public class PaymentController {
    private final PaymentInitiationService paymentInitiationService;
    private final PaymentService paymentService;

    public record CreatePaymentRequest(
            @JsonProperty("idempotency_key") @NotBlank String idempotencyKey,
            @JsonProperty("amount_minor") @Positive long amountMinor,
            @JsonProperty("currency") @NotBlank String currency,
            @JsonProperty("provider") @NotBlank String provider
    ) {}

    public record CreatePaymentResponse(
            @JsonProperty("payment_id") UUID paymentId,
            @JsonProperty("status") String status,
            @JsonProperty("amount_minor") long amountMinor,
            @JsonProperty("currency") String currency,
            @JsonProperty("redirect_url") String redirectUrl
    ) {}

    public record PaymentDetailResponse(
            @JsonProperty("payment_id") UUID paymentId,
            @JsonProperty("status") String status,
            @JsonProperty("amount_minor") long amountMinor,
            @JsonProperty("currency") String currency,
            @JsonProperty("version") int version
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        PaymentInitiationService.InitiatePaymentResult result = paymentInitiationService.initiate(
                request.idempotencyKey(),
                request.amountMinor(),
                request.currency(),
                request.provider(),
                clientIp
        );
        Payment payment = result.payment();
        return new CreatePaymentResponse(
                payment.paymentId(),
                payment.status().name(),
                payment.amountMinor(),
                payment.currency(),
                result.redirectUrl()
        );
    }

    @GetMapping("/{id}")
    public PaymentDetailResponse getPaymentDetail(@PathVariable UUID id) {
        Payment payment = paymentService.getById(id);
        return new PaymentDetailResponse(
                payment.paymentId(),
                payment.status().name(),
                payment.amountMinor(),
                payment.currency(),
                payment.version()
        );
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public void refundPayment(@PathVariable UUID id) {
        throw new BusinessException(CommonErrorCode.COMMON_BAD_REQUEST,
                "Refund not implemented", null, Map.of("paymentId", id.toString()));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
