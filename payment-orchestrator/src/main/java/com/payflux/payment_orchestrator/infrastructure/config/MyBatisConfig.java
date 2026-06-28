package com.payflux.payment_orchestrator.infrastructure.config;

import com.payflux.core.mybatis.RlsMyBatisInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {
    @Bean
    public ConfigurationCustomizer rlsConfigurationCustomizer() {
        return config -> config.addInterceptor(new RlsMyBatisInterceptor());
    }
}
