package com.payflux.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payflux.shared")
public record SharedLibsProperties(Web web, Observability observability) {
    public record Web(boolean exceptionHandlerEnabled, boolean wrapResponseEnabled) {
        public Web {

        }
    }

    public record Observability(boolean correlationIdEnabled,boolean executionTimeAspectEnabled) {}

    public SharedLibsProperties {
        if (web == null) web = new Web(true, true);
        if (observability == null) observability = new Observability(true, true);
    }
}
