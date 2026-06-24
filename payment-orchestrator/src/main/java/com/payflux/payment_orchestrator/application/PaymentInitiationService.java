package com.payflux.payment_orchestrator.application;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.BusinessException;
import com.payflux.exception.InfrastructureException;
import com.payflux.grpc.v1.Money;
import com.payflux.grpc.v1.PaymentProviderServiceGrpc;
import com.payflux.grpc.v1.ProcessPaymentRequest;
import com.payflux.grpc.v1.ProcessPaymentResponse;
import com.payflux.payment_orchestrator.domain.Payment;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentInitiationService {
    private static final Logger log = LoggerFactory.getLogger(PaymentInitiationService.class);
    private final PaymentService paymentService;

    @GrpcClient("vnpay-adapter")
    private PaymentProviderServiceGrpc.PaymentProviderServiceBlockingStub vnpayStub;

    public record InitiatePaymentResult(Payment payment, String redirectUrl) {
    }

    public InitiatePaymentResult initiate(
            String idempotencyKey,
            long amountMinor,
            String currency,
            String provider,
            String clientIp
    ) {
        Payment payment = paymentService.createPending(idempotencyKey, amountMinor, currency, provider);
        if (!"vnpay".equalsIgnoreCase(provider)) {
            throw new BusinessException(CommonErrorCode.COMMON_BAD_REQUEST,
                    "Unsupported provider: " + provider, null,
                    Map.of("provider", provider));
        }
        String paymentId = payment.paymentId().toString();
        String ip = (clientIp == null || clientIp.isBlank()) ? "127.0.0.1" : clientIp;

        ProcessPaymentRequest gpcRequest = ProcessPaymentRequest.newBuilder()
                .setMerchantId(payment.merchantId().toString())
                .setIdempotencyKey(payment.idempotencyKey())
                .setAmount(Money.newBuilder()
                        .setCurrency(currency)
                        .setAmountMinor(amountMinor)
                        .build()
                )
                .putMetadata("payment_id", paymentId)
                .putMetadata("client_ip", ip)
                .build();

        try {
            ProcessPaymentResponse grpcResponse = vnpayStub.processPayment(gpcRequest);
            log.info("Payment initiated: paymentId={} providerTxnId={}", paymentId, grpcResponse.getProviderTxnId());
            return new InitiatePaymentResult(payment, grpcResponse.getRedirectUrl());
        } catch (StatusRuntimeException e) {
            log.error("gRPC processPayment failed paymentId={} status={}",
                    paymentId, e.getStatus().getCode(), e);
            throw new InfrastructureException(
                    CommonErrorCode.COMMON_SERVICE_UNAVAILABLE,
                    "vnpay-adapter unavailable: " + e.getStatus().getCode(),
                    e,
                    Map.of("grpcStatus", e.getStatus().getCode().name(), "paymentId", paymentId));
        }
    }
}
