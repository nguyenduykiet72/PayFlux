package com.payflux.payment_orchestrator.infrastructure.config;

import com.payflux.core.grpc.TenantGrpcClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {
    @GrpcGlobalClientInterceptor
    public TenantGrpcClientInterceptor tenantGrpcClientInterceptor() {
        return new TenantGrpcClientInterceptor();
    }
}
