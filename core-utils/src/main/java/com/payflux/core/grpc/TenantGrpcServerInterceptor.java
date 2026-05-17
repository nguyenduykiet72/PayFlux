package com.payflux.core.grpc;

import com.payflux.core.tenant.TenantContext;
import io.grpc.*;

import java.util.UUID;

/**
 * Server-side interceptor: extracts x-merchant-id from gRPC metadata,
 * binds it to TenantContext (ScopedValue) for the duration of each callback.
 *
 * Why wrap each callback individually? gRPC callbacks (onMessage, onHalfClose, etc.)
 * execute on the executor pool, NOT on the interceptCall() thread. If we only bind
 * ScopedValue in interceptCall(), the scope ends when that method returns, and all
 * subsequent callbacks would have no tenant context. This is the most common trap
 * when migrating from ThreadLocal to ScopedValue with gRPC.
 */
public class TenantGrpcServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String raw = headers.get(TenantMetadataKeys.MERCHANT_ID);
        if (raw == null || raw.isBlank()) {
            call.close(Status.UNAUTHENTICATED.withDescription("missing x-merchant-id"), headers);
            return new ServerCall.Listener<>() {};
        }

        UUID merchantId;
        try {
            merchantId = UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            call.close(Status.INVALID_ARGUMENT.withDescription("x-merchant-id not a valid UUID"), headers);
            return new ServerCall.Listener<>() {};
        }

        ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
        return new TenantBindingListener<>(delegate, merchantId);
    }

    private static final class TenantBindingListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final UUID merchantId;

        TenantBindingListener(ServerCall.Listener<ReqT> delegate, UUID merchantId) {
            super(delegate);
            this.merchantId = merchantId;
        }

        @Override
        public void onMessage(ReqT message) {
            TenantContext.runWhere(merchantId, () -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            TenantContext.runWhere(merchantId, super::onHalfClose);
        }

        @Override
        public void onCancel() {
            TenantContext.runWhere(merchantId, super::onCancel);
        }

        @Override
        public void onComplete() {
            TenantContext.runWhere(merchantId, super::onComplete);
        }

        @Override
        public void onReady() {
            TenantContext.runWhere(merchantId, super::onReady);
        }
    }
}
