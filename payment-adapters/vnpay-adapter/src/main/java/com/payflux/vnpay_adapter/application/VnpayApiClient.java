package com.payflux.vnpay_adapter.application;

import com.payflux.adapter.commons.crypto.HmacSha512Signer;
import com.payflux.adapter.commons.http.ResilientRestClientBuilder;
import com.payflux.vnpay_adapter.config.VnpayProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class VnpayApiClient {
    private static final Logger log = LoggerFactory.getLogger(VnpayApiClient.class);
    private static final DateTimeFormatter VNPAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final String COMMAND_QUERY = "querydr";
    private static final String IP_FALLBACK = "127.0.0.1";

    private final VnpayProperties properties;
    private final HmacSha512Signer signer;
    private final RestClient restClient;

    public VnpayApiClient(VnpayProperties properties, HmacSha512Signer signer) {
        this.properties = properties;
        this.signer = signer;
        this.restClient = ResilientRestClientBuilder.create()
                .baseUrl(properties.apiUrl())
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @CircuitBreaker(name = "vnpay", fallbackMethod = "queryFallback")
    @Retry(name = "vnpay")
    public QueryResult query(String txnRef, String transactionDate) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = VNPAY_DATE_FORMATTER.format(Instant.now());
        String orderInfo = "Query " + txnRef;

        String canonical = String.join("|",
                requestId,
                properties.version(),
                COMMAND_QUERY,
                properties.tmnCode(),
                txnRef,
                transactionDate,
                createDate,
                IP_FALLBACK,
                orderInfo
        );

        String signature = signer.hmacHex(canonical);

        Map<String, String> body = new LinkedHashMap();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", properties.version());
        body.put("vnp_Command", COMMAND_QUERY);
        body.put("vnp_TmnCode", properties.tmnCode());
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", IP_FALLBACK);
        body.put("vnp_SecureHash", signature);

        log.debug("VNPay query txnRef={} transactionDate={} requestId={}", txnRef, transactionDate, requestId);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .body(body)
                .retrieve()
                .body(Map.class);

        return QueryResult.from(response);
    }

    @SuppressWarnings("unused")
    private QueryResult queryFallback(String txnRef, String transactionDate, Throwable cause) {
        log.warn("VNPay query txnRef={} transactionDate={} cause={} failed", txnRef, transactionDate, cause.toString());
        return QueryResult.unavailable();
    }

    public record QueryResult(String responseCode, String transactionStatus, boolean isAvailable) {
        static QueryResult from(Map<String, Object> body) {
            if (body == null) return unavailable();
            String code = String.valueOf(body.getOrDefault("vnp_ResponseCode", ""));
            String staus = String.valueOf(body.getOrDefault("vnp_TransactionStatus", ""));
            return new QueryResult(code, staus, true);
        }

        static QueryResult unavailable() {
            return new QueryResult("", "", false);
        }
    }
}
