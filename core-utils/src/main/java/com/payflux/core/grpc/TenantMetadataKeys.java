package com.payflux.core.grpc;

import io.grpc.Metadata;

public final class TenantMetadataKeys {
    public static final Metadata.Key<String> MERCHANT_ID =
            Metadata.Key.of("x-merchant-id", Metadata.ASCII_STRING_MARSHALLER);

    private TenantMetadataKeys() {}
}
