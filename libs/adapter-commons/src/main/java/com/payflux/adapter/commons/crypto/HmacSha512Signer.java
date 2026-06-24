package com.payflux.adapter.commons.crypto;

import com.payflux.errorcode.CommonErrorCode;
import com.payflux.exception.InfrastructureException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class HmacSha512Signer {
    private static final String ALGORITHM = "HmacSHA512";
    private final byte[] secretKeyBytes;

    public HmacSha512Signer(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("HMAC secret key cannot be null or blank");
        }
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(Map<String, String> params) {
        SortedMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!first)
                hashData.append("&");
            hashData.append(entry.getKey()).append("=").append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            first = false;
        }
        String canonical = hashData.toString();
        return hmac(canonical);
    }

    /**
     * Verify a received signature against re-computed signature.
     * Uses constant-time comparison (MessageDigest.isEqual) to prevent timing
     * attacks.
     *
     * @param params            payload params (must NOT contain vnp_SecureHash or
     *                          vnp_SecureHashType)
     * @param receivedSignature signature from request (case-insensitive)
     * @return true if valid
     */
    public boolean verify(Map<String, String> params, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isEmpty()) {
            return false;
        }
        String expectedSignature = sign(params);
        byte[] expectedBytes = expectedSignature.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] receivedBytes = receivedSignature.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expectedBytes, receivedBytes);
    }

    public String hmacHex(String data) {
        return hmac(data);
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, ALGORITHM));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new InfrastructureException(CommonErrorCode.COMMON_INTERNAL_ERROR, "HMAC-SHA512 signing failed", e,
                    Map.of("algorithm", ALGORITHM));
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

}
