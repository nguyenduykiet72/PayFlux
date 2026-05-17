package com.payflux.vnpay_adapter.presentation;

import static io.grpc.MethodDescriptor.newBuilder;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.payflux.grpc.v1.CheckStatusRequest;
import com.payflux.grpc.v1.CheckStatusResponse;
import com.payflux.grpc.v1.PaymentStatus;
import com.payflux.grpc.v1.ProcessPaymentRequest;
import com.payflux.grpc.v1.ProcessPaymentResponse;
import com.payflux.grpc.v1.RefundPaymentRequest;
import com.payflux.grpc.v1.RefundPaymentResponse;
import com.payflux.grpc.v1.PaymentProviderServiceGrpc.PaymentProviderServiceImplBase;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class VnpayPaymentService extends PaymentProviderServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentService.class);

    @Override
    public void processPayment(ProcessPaymentRequest request, StreamObserver<ProcessPaymentResponse> observer) {
        log.info("processPayment idempotencyKey: {}", request.getIdempotencyKey());

        var response = ProcessPaymentResponse.newBuilder().setPaymentId(UUID.randomUUID().toString())
                .setProviderTxnId("VNPAY-MOCK-" + UUID.randomUUID())
                .setStatus(PaymentStatus.PAYMENT_STATUS_AUTHORIZED)
                .setRedirectUrl("https://sandbox.vnpayment.vn/mock/" + request.getIdempotencyKey())
                .build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void refundPayment(RefundPaymentRequest request, StreamObserver<RefundPaymentResponse> observer) {
        log.info("refundPayment paymentId: {}", request.getPaymentId());

        var response = RefundPaymentResponse.newBuilder()
                .setRefundId("VNPAY-REFUND-MOCK-" + UUID.randomUUID())
                .setStatus(PaymentStatus.PAYMENT_STATUS_REFUNDED)
                .build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void checkStatus(CheckStatusRequest request, StreamObserver<CheckStatusResponse> observer) {
        log.info("checkStatus paymentId: {}", request.getPaymentId());

        var response = CheckStatusResponse.newBuilder()
                .setStatus(PaymentStatus.PAYMENT_STATUS_CAPTURED)
                .setProviderTxnId("VNPAY-MOCK-" + request.getPaymentId())
                .build();

        observer.onNext(response);
        observer.onCompleted();
    }

}
