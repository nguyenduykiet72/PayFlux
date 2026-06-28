package com.payflux.payment_orchestrator.infrastructure.web;

import com.payflux.core.web.TenantServletFilter;
import com.payflux.payment_orchestrator.application.MerchantResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<PublicApiTenantFilter> publicApiTenantFilter(MerchantResolver merchantResolver) {
        var reg = new FilterRegistrationBean<>(new PublicApiTenantFilter(merchantResolver));
        reg.addUrlPatterns("/v1/payments","/v1/payments/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<TenantServletFilter> tenantFilter() {
        var reg = new FilterRegistrationBean<>(new TenantServletFilter());
        reg.addUrlPatterns("/internal/*","/v1/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
