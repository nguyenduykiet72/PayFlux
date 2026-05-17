package com.payflux.payment_orchestrator.presentation;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.InfrastructureException;
import com.payflux.grpc.v1.Money;
import com.payflux.grpc.v1.PaymentProviderServiceGrpc;
import com.payflux.grpc.v1.ProcessPaymentRequest;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;

@RestController
@RequestMapping("/internal/payments")
public class InternalTestController {

    private static final Logger log = LoggerFactory.getLogger(InternalTestController.class);

    @GrpcClient("vnpay-adapter")
    private PaymentProviderServiceGrpc.PaymentProviderServiceBlockingStub vnpayStub;

    public record TestPaymentResponse(
            String paymentId,
            String providerTxnId,
            String status,
            String redirectUrl) {
    }

    @PostMapping("/test")
    public TestPaymentResponse test() {
        var request = ProcessPaymentRequest.newBuilder()
                .setMerchantId(UUID.randomUUID().toString())
                .setIdempotencyKey("test-" + UUID.randomUUID())
                .setAmount(Money.newBuilder()
                        .setCurrency("VND")
                        .setAmountMinor(100_000L)
                        .build())
                .setReturnUrl("https://merchant.example/return")
                .build();

        try {
            var response = vnpayStub.processPayment(request);
            return new TestPaymentResponse(
                    response.getPaymentId(),
                    response.getProviderTxnId(),
                    response.getStatus().name(),
                    response.getRedirectUrl());
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed: status={} description={}",
                    e.getStatus().getCode(), e.getStatus().getDescription(), e);
            throw new InfrastructureException(
                    CommonErrorCode.COMMON_SERVICE_UNAVAILABLE,
                    "vnpay-adapter unavailable: " + e.getStatus().getCode(),
                    e,
                    Map.of("grpcStatus", e.getStatus().getCode().name()));
        }
    }
}
