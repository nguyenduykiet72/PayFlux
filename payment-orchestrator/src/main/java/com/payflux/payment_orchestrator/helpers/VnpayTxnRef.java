package com.payflux.payment_orchestrator.helpers;

import java.util.UUID;

public final class VnpayTxnRef {
    private static final int TXN_REF_LENGTH = 32;

    private VnpayTxnRef() {}

    public static String encode(UUID paymentId) {
        String stripped = paymentId.toString().replace("-", "");
        if (stripped.length() != TXN_REF_LENGTH) {
            throw new IllegalArgumentException("Invalid paymentId");
        }
        return stripped;
    }

    public static UUID decode(String txnRef) {
        if(txnRef == null || txnRef.length() != TXN_REF_LENGTH) {
            throw new IllegalArgumentException("Invalid txnRef");
        }
        String withDashes = txnRef.substring(0, 8) + "-" +
                txnRef.substring(8, 12) + "-" +
                txnRef.substring(12, 16) + "-" +
                txnRef.substring(16, 20) + "-" +
                txnRef.substring(20);
        return UUID.fromString(withDashes);
    }
}
