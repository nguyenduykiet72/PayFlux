package com.payflux.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties( SharedLibsProperties.class)
@Import({
    WebExceptionAutoConfiguration.class,
    ObservabilityAutoConfiguration.class
})
public class SharedLibsAutoConfiguration {
}
