package com.payflux.payment_orchestrator.infrastructure.config;

import com.payflux.core.web.TenantServletFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<TenantServletFilter> tenantFilter() {
        var reg = new FilterRegistrationBean<>(new TenantServletFilter());
        reg.addUrlPatterns("/internal/*","/v1/*");
        reg.setOrder(TenantServletFilter.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
