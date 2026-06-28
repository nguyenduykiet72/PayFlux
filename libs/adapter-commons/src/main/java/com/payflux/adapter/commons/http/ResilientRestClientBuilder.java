package com.payflux.adapter.commons.http;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

public final class ResilientRestClientBuilder {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    private String baseUrl;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;

    private ResilientRestClientBuilder() {
    }

    public static ResilientRestClientBuilder create() {
        return new ResilientRestClientBuilder();
    }

    public ResilientRestClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ResilientRestClientBuilder connectTimeout(Duration connectTimeout) {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("Connect timeout must be a positive duration: " + connectTimeout);
        }
        this.connectTimeout = connectTimeout;
        return this;
    }

    public ResilientRestClientBuilder readTimeout(Duration readTimeout) {
        if (readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("Read timeout must be a positive duration: " + readTimeout);
        }
        this.readTimeout = readTimeout;
        return this;
    }

    public RestClient build() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .version(HttpClient.Version.HTTP_2)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory);

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }
}
