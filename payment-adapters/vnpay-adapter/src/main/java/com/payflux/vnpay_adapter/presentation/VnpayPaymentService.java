package com.payflux.vnpay_adapter.presentation;

import java.util.UUID;

import com.payflux.vnpay_adapter.application.TxnDateCache;
import com.payflux.vnpay_adapter.application.VnpayApiClient;
import com.payflux.vnpay_adapter.application.VnpayUrlBuilder;
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
    private static final String DEFAULT_CLIENT_IP = "127.0.0.1";
    private static final int VNPAY_TXN_REF_MAX_LENGTH = 32;
    private final VnpayUrlBuilder vnpayUrlBuilder;
    private final VnpayApiClient vnpayApiClient;
    private final TxnDateCache txnDateCache;

    public VnpayPaymentService(VnpayUrlBuilder vnpayUrlBuilder, VnpayApiClient vnpayApiClient, TxnDateCache txnDateCache) {
        this.vnpayUrlBuilder = vnpayUrlBuilder;
        this.vnpayApiClient = vnpayApiClient;
        this.txnDateCache = txnDateCache;
    }

    @Override
    public void processPayment(ProcessPaymentRequest request, StreamObserver<ProcessPaymentResponse> observer) {
        String paymentId = resolvePaymentId(request);
        long amountMinor = request.getAmount().getAmountMinor();
        String txnRef = deriveTxnRef(paymentId);
        String clientIp = request.getMetadataOrDefault("client_ip", DEFAULT_CLIENT_IP);
        String orderInfo = request.getMetadataOrDefault("order_info", "Payment " + txnRef);
        log.info("processPayment idempotencyKey={} txnRef={} amountMinor={}",
                paymentId, txnRef, amountMinor);

        VnpayUrlBuilder.BuildResult result = vnpayUrlBuilder.build(
                txnRef, amountMinor, orderInfo, clientIp);
        txnDateCache.put(txnRef, result.createDate());

        var response = ProcessPaymentResponse.newBuilder()
                .setPaymentId(paymentId)
                .setProviderTxnId(txnRef)
                .setStatus(PaymentStatus.PAYMENT_STATUS_PENDING)
                .setRedirectUrl(result.paymentUrl())
                .build();

        observer.onNext(response);
        observer.onCompleted();
    }

    private static String deriveTxnRef(String idempotencyKey) {
        String stripped = idempotencyKey.replace("-", "");
        return stripped.length() <= VNPAY_TXN_REF_MAX_LENGTH
                ? stripped
                : stripped.substring(0, VNPAY_TXN_REF_MAX_LENGTH);
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
        String paymentId = request.getPaymentId();
        String txnRef = deriveTxnRef(paymentId);
        String transactionDate = txnDateCache.get(txnRef);
        log.info("checkStatus paymentId={} txnRef={} transactionDate={}", paymentId, txnRef, transactionDate);

        if (transactionDate == null) {
            observer.onNext(CheckStatusResponse.newBuilder()
                    .setStatus(PaymentStatus.PAYMENT_STATUS_UNSPECIFIED)
                    .setProviderTxnId(txnRef)
                    .build());
            observer.onCompleted();
            return;
        }

        VnpayApiClient.QueryResult result = vnpayApiClient.query(txnRef, transactionDate);
        PaymentStatus status = mapStatus(result);

        observer.onNext(CheckStatusResponse.newBuilder()
                .setStatus(status)
                .setProviderTxnId(txnRef)
                .build());
        observer.onCompleted();
    }

    private static String resolvePaymentId(ProcessPaymentRequest request) {
        String formMeta = request.getMetadataOrDefault("payment_id","");
        if(!formMeta.isBlank()) return formMeta;
        return request.getIdempotencyKey();
    }

    private static PaymentStatus mapStatus(VnpayApiClient.QueryResult result) {
        if (!result.isAvailable()) return PaymentStatus.PAYMENT_STATUS_UNSPECIFIED;
        if ("00".equals(result.responseCode()) && "00".equals(result.transactionStatus())) {
            return PaymentStatus.PAYMENT_STATUS_CAPTURED;
        }
        if ("01".equals(result.transactionStatus())) {
            return PaymentStatus.PAYMENT_STATUS_PENDING;
        }
        return PaymentStatus.PAYMENT_STATUS_FAILED;
    }

}
