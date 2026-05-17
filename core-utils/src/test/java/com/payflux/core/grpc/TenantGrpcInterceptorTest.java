package com.payflux.core.grpc;

import com.payflux.core.tenant.TenantContext;
import io.grpc.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class TenantGrpcInterceptorTest {

    // --- Server interceptor tests ---

    @Test
    void serverInterceptor_missingHeader_closesWithUnauthenticated() {
        var interceptor = new TenantGrpcServerInterceptor();
        var call = new NoOpServerCall<String, String>();
        var metadata = new Metadata();

        interceptor.interceptCall(call, metadata, new ServerCallHandler<>() {
            @Override
            public ServerCall.Listener<String> startCall(ServerCall<String, String> c, Metadata h) {
                fail("Should not reach handler when header is missing");
                return new ServerCall.Listener<>() {};
            }
        });

        assertThat(call.closedStatus).isNotNull();
        assertThat(call.closedStatus.getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void serverInterceptor_invalidUuid_closesWithInvalidArgument() {
        var interceptor = new TenantGrpcServerInterceptor();
        var call = new NoOpServerCall<String, String>();
        var metadata = new Metadata();
        metadata.put(TenantMetadataKeys.MERCHANT_ID, "not-a-uuid");

        interceptor.interceptCall(call, metadata, new ServerCallHandler<>() {
            @Override
            public ServerCall.Listener<String> startCall(ServerCall<String, String> c, Metadata h) {
                fail("Should not reach handler for invalid UUID");
                return new ServerCall.Listener<>() {};
            }
        });

        assertThat(call.closedStatus).isNotNull();
        assertThat(call.closedStatus.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void serverInterceptor_validUuid_bindsTenantInCallbacks() {
        var interceptor = new TenantGrpcServerInterceptor();
        var call = new NoOpServerCall<String, String>();
        var metadata = new Metadata();
        UUID merchantId = UUID.randomUUID();
        metadata.put(TenantMetadataKeys.MERCHANT_ID, merchantId.toString());

        AtomicReference<UUID> capturedInHalfClose = new AtomicReference<>();

        var listener = interceptor.interceptCall(call, metadata, new ServerCallHandler<>() {
            @Override
            public ServerCall.Listener<String> startCall(ServerCall<String, String> c, Metadata h) {
                return new ServerCall.Listener<>() {
                    @Override
                    public void onHalfClose() {
                        capturedInHalfClose.set(TenantContext.get());
                    }
                };
            }
        });

        listener.onHalfClose();
        assertThat(capturedInHalfClose.get()).isEqualTo(merchantId);
    }

    // --- Client interceptor tests ---

    @Test
    void clientInterceptor_attachesMerchantIdWhenBound() {
        var interceptor = new TenantGrpcClientInterceptor();
        UUID merchantId = UUID.randomUUID();
        AtomicReference<String> capturedHeader = new AtomicReference<>();

        TenantContext.runWhere(merchantId, () -> {
            interceptor.interceptCall(
                MethodDescriptor.<String, String>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName("test/method")
                    .setRequestMarshaller(new StringMarshaller())
                    .setResponseMarshaller(new StringMarshaller())
                    .build(),
                CallOptions.DEFAULT,
                new Channel() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                            MethodDescriptor<ReqT, RespT> m, CallOptions o) {
                        return new NoOpClientCall<>() {
                            @Override
                            public void start(Listener<RespT> listener, Metadata headers) {
                                capturedHeader.set(headers.get(TenantMetadataKeys.MERCHANT_ID));
                            }
                        };
                    }

                    @Override
                    public String authority() { return "test"; }
                }
            ).start(new ClientCall.Listener<>() {}, new Metadata());
        });

        assertThat(capturedHeader.get()).isEqualTo(merchantId.toString());
    }

    @Test
    void clientInterceptor_doesNotAttachWhenNotBound() {
        var interceptor = new TenantGrpcClientInterceptor();
        AtomicReference<String> capturedHeader = new AtomicReference<>();

        interceptor.interceptCall(
            MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("test/method")
                .setRequestMarshaller(new StringMarshaller())
                .setResponseMarshaller(new StringMarshaller())
                .build(),
            CallOptions.DEFAULT,
            new Channel() {
                @Override
                public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                        MethodDescriptor<ReqT, RespT> m, CallOptions o) {
                    return new NoOpClientCall<>() {
                        @Override
                        public void start(Listener<RespT> listener, Metadata headers) {
                            capturedHeader.set(headers.get(TenantMetadataKeys.MERCHANT_ID));
                        }
                    };
                }

                @Override
                public String authority() { return "test"; }
            }
        ).start(new ClientCall.Listener<>() {}, new Metadata());

        assertThat(capturedHeader.get()).isNull();
    }

    // --- Test helpers ---

    private static class NoOpServerCall<ReqT, RespT> extends ServerCall<ReqT, RespT> {
        Status closedStatus;

        @Override public void request(int numMessages) {}
        @Override public void sendHeaders(Metadata headers) {}
        @Override public void sendMessage(RespT message) {}
        @Override public void close(Status status, Metadata trailers) { this.closedStatus = status; }
        @Override public boolean isCancelled() { return false; }
        @Override public MethodDescriptor<ReqT, RespT> getMethodDescriptor() { return null; }
    }

    private static class NoOpClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        @Override public void start(Listener<RespT> listener, Metadata headers) {}
        @Override public void request(int numMessages) {}
        @Override public void cancel(String message, Throwable cause) {}
        @Override public void halfClose() {}
        @Override public void sendMessage(ReqT message) {}
    }

    private static class StringMarshaller implements io.grpc.MethodDescriptor.Marshaller<String> {
        @Override
        public java.io.InputStream stream(String value) {
            return new java.io.ByteArrayInputStream(value.getBytes());
        }

        @Override
        public String parse(java.io.InputStream stream) {
            try { return new String(stream.readAllBytes()); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        }
    }
}
