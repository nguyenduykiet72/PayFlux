package com.payflux.vnpay_adapter.application;

import com.payflux.adapter.commons.crypto.HmacSha512Signer;
import com.payflux.vnpay_adapter.config.VnpayProperties;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VnpayUrlBuilder {
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final VnpayProperties vnpayProperties;
    private final HmacSha512Signer signer;

    public VnpayUrlBuilder(VnpayProperties vnpayProperties, HmacSha512Signer signer) {
        this.vnpayProperties = vnpayProperties;
        this.signer = signer;
    }

    public BuildResult build(String txnRef, long amountMinor, String orderInfo, String clientIp) {
        Map<String, String> params = buildParams(txnRef,amountMinor,orderInfo, clientIp);
        String signature = signer.sign(params);
        String queryString = encodeAsQueryString(params);
        String url = vnpayProperties.apiUrl() + "?" + queryString + "&vnp_SecureHash=" + signature;
        return new BuildResult(url, params.get("vnp_CreateDate"));
    }

    public record BuildResult(String paymentUrl, String createDate) {}

    private Map<String, String> buildParams(String txnRef, long amountMinor, String orderInfo, String clientIp) {
        Map<String, String> params = new LinkedHashMap<>();
        Instant now = Instant.now();

        params.put("vnp_Version", vnpayProperties.version());
        params.put("vnp_Command", vnpayProperties.command());
        params.put("vnp_TmnCode", vnpayProperties.tmnCode());
        params.put("vnp_Amount", String.valueOf(amountMinor * 100L));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.returnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", VNPAY_DATE_FORMAT.format(now));
        params.put("vnp_ExpireDate", VNPAY_DATE_FORMAT.format(now.plus(vnpayProperties.paymentExpiry())));

        return params;
    }

    /**
     * Encode params as URL query string. Encodes BOTH keys and values to be
     * safe (VNPay keys are alphanumeric+underscore so encoding them is a no-op,
     * but explicit is better than implicit).
     */
    private static String encodeAsQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            first = false;
        }
        return sb.toString();
    }
}
