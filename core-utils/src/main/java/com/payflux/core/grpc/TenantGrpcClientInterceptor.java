package com.payflux.core.grpc;

import com.payflux.core.tenant.TenantContext;
import io.grpc.*;

/**
 * Client-side interceptor: attaches current tenant's merchant_id from
 * TenantContext (ScopedValue) into outgoing gRPC metadata.
 *
 * This runs on the caller thread where ScopedValue is already bound
 * (e.g., inside the HTTP filter scope), so no special wrapping needed.
 */
public class TenantGrpcClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (TenantContext.isBound()) {
                    headers.put(TenantMetadataKeys.MERCHANT_ID, TenantContext.get().toString());
                }
                super.start(responseListener, headers);
            }
        };
    }
}
