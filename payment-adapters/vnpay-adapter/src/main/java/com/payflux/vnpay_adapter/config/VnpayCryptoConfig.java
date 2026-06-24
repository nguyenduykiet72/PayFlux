package com.payflux.vnpay_adapter.config;

import com.payflux.adapter.commons.crypto.HmacSha512Signer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VnpayCryptoConfig {
    @Bean
    public HmacSha512Signer vnpaySigner(VnpayProperties vnpayProperties) {
        return new HmacSha512Signer(vnpayProperties.hashSecret());
    }
}
