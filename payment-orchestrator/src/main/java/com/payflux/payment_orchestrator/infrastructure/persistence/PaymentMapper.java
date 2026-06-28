package com.payflux.payment_orchestrator.infrastructure.persistence;

import com.payflux.core.state.PaymentState;
import com.payflux.payment_orchestrator.domain.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface PaymentMapper {
    int insert(Payment payment);

    Optional<Payment> findById(@Param("paymentId") UUID paymentId);

    Optional<Payment> findByIdempotencyKey(@Param("merchantId") UUID merchantId,
            @Param("idempotencyKey") String idempotencyKey);

    Optional<Payment> findByTxnRefForIpn(@Param("txnRef") String txnRef);

    int updateStatus(@Param("paymentId") UUID paymentId,
            @Param("newStatus") PaymentState newStatus,
            @Param("expectedVersion") int expectedVersion);
}