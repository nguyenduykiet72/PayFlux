package com.payflux.payment_orchestrator.application;

import com.payflux.adapter.commons.crypto.HmacSha512Signer;
import com.payflux.core.state.PaymentState;
import com.payflux.core.tenant.TenantContext;
import com.payflux.payment_orchestrator.domain.Payment;
import com.payflux.payment_orchestrator.infrastructure.persistence.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentIpnService {
    private static final Logger log = LoggerFactory.getLogger(PaymentIpnService.class);
    private static final String VPN_SUCCESS = "00";

    private final HmacSha512Signer vnpaySigner;
    private final PaymentMapper paymentMapper;
    private final PaymentService paymentService;

    public record IpnResult(String rspCode, String message) {}

    public IpnResult handle(Map<String, String> rawParams) {
        Map<String, String> params = new HashMap<>(rawParams);
        String receiveHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        if(receiveHash == null || !vnpaySigner.verify(params, receiveHash)) {
            log.warn("VNPay IPN verification invalid txnRef={}", params.get("vnp_TxnRef"));
            return new IpnResult("97", "Invalid signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        if(txnRef == null || txnRef.isBlank()) {
            return new IpnResult("99", "Missing txnRef");
        }

        Payment payment = paymentMapper.findByTxnRefForIpn(txnRef).orElse(null);
        if (payment == null ) {
            log.warn("VNPay IPN payment not found txnRef={}", txnRef);
            return new IpnResult("01", "Payment not found");
        }

        PaymentState target = VPN_SUCCESS.equals(responseCode)
                ? PaymentState.CAPTURED
                : PaymentState.FAILED;

        if (payment.status() == target) {
            log.info("VNPay IPN payment already in state txnRef={} state={}", txnRef, payment.status());
            return new IpnResult("00", "Payment confirm success");
        }

        try {
            TenantContext.runWhere(payment.merchantId(), () ->
                paymentService.transitionTo(payment.paymentId(),target)
            );
            log.info("VNPay IPN payment transitioned txnRef={} state={}", txnRef, target);
            return new IpnResult("00", "Payment confirm success");
        } catch (Exception e) {
            log.error("VNPay IPN payment transition failed txnRef={} state={}", txnRef, target, e);
            return new IpnResult("99", "Payment confirm failed");
        }
    }


}
