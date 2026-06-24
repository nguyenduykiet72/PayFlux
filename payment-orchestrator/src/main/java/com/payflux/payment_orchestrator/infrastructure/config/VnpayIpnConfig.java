package com.payflux.payment_orchestrator.infrastructure.config;

import com.payflux.adapter.commons.crypto.HmacSha512Signer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VnpayIpnProperties.class)
public class VnpayIpnConfig {
    @Bean
    public HmacSha512Signer vnpayIpnSigner(VnpayIpnProperties vnpayIpnProperties) {
        return new HmacSha512Signer(vnpayIpnProperties.hashSecret());
    }
}
