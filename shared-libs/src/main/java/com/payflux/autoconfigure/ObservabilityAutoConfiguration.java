package com.payflux.autoconfigure;

import com.payflux.observability.CorrelationFilter;
import com.payflux.observability.LogExecutionTimeAspect;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ObservabilityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "payflux.observability",
            name = "correlation-id-enabled",
            havingValue = "true",
            matchIfMissing = true)
    public CorrelationFilter correlationFilter() {
        return new CorrelationFilter();
    }


    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(prefix = "payflux.observability",
            name = "execution-time-aspect-enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LogExecutionTimeAspect logExecutionTimeAspect(ObservationRegistry registry) {
        return new LogExecutionTimeAspect(registry);
    }
}
