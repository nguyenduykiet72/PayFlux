package com.payflux.adapter.commons.crypto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacSha512SignerTest {

    private static final String TEST_SECRET = "test-secret-key";
    private final HmacSha512Signer signer = new HmacSha512Signer(TEST_SECRET);

    @Test
    void signProducesValidHexFormat() {
        Map<String, String> params = Map.of("a", "1");

        String sig = signer.sign(params);

        assertThat(sig)
                .as("HMAC-SHA512 hex output must be exactly 128 lowercase hex chars")
                .hasSize(128)
                .matches("[0-9a-f]+");
    }

    @Test
    void signMatchesGoldenVectorSimple() {
        // Generated via Python:
        //   hmac.new(b"test-secret-key", b"a=1&b=2&c=hello", hashlib.sha512).hexdigest()
        Map<String, String> params = new HashMap<>();
        params.put("a", "1");
        params.put("b", "2");
        params.put("c", "hello");

        String sig = signer.sign(params);

        assertThat(sig).isEqualTo(
                "02324e5337cdd692411841b716822b3bb896397c1a8cf2b67234ddf7a5d2b1c0"
                        + "f2b966070b84988268439aa6847ff2df138868164f97cbfd6f713ea963ebfd07");
    }

    @Test
    void signMatchesGoldenVectorWithUrlEncoding() {
        // Generated via Python (urllib.parse.quote_plus on values, sorted keys):
        //   canonical = "vnp_Amount=1000000&vnp_OrderInfo=Thanh+toan+don+hang%3A5"
        //   hmac.new(b"test-secret-key", canonical.encode('utf-8'), hashlib.sha512).hexdigest()
        Map<String, String> params = new HashMap<>();
        params.put("vnp_OrderInfo", "Thanh toan don hang:5");
        params.put("vnp_Amount", "1000000");

        String sig = signer.sign(params);

        assertThat(sig).isEqualTo(
                "f157e4c90e746e5473285112f76ded036151776d1cac3816ca021b2f661bf4d8"
                        + "e4bf33c67c3322cd92094dda8a32515eac814f00bb2107b8c37d79975d4bd1fc");
    }

    @Test
    void signSkipsNullAndEmptyValues() {
        // Spec VNPay: skip params with null/empty value when computing hash.
        // Adding empty/null entries must NOT change the signature.
        Map<String, String> withEmpty = new HashMap<>();
        withEmpty.put("a", "1");
        withEmpty.put("b", "");
        withEmpty.put("c", null);

        Map<String, String> withoutEmpty = Map.of("a", "1");

        assertThat(signer.sign(withEmpty))
                .as("Empty/null values must be skipped to match VNPay spec")
                .isEqualTo(signer.sign(withoutEmpty));
    }

    @Test
    void verifyReturnsTrueForValidSignature() {
        Map<String, String> params = Map.of(
                "vnp_TxnRef", "PAY12345",
                "vnp_ResponseCode", "00",
                "vnp_Amount", "100000");

        String sig = signer.sign(params);

        assertThat(signer.verify(params, sig)).isTrue();
    }

    @Test
    void verifyReturnsFalseForTamperedParam() {
        Map<String, String> original = Map.of("vnp_Amount", "100000", "vnp_TxnRef", "PAY1");
        String validSig = signer.sign(original);

        Map<String, String> tampered = Map.of("vnp_Amount", "999999", "vnp_TxnRef", "PAY1");

        assertThat(signer.verify(tampered, validSig))
                .as("Verify must fail when any param differs from signed payload")
                .isFalse();
    }

    @Test
    void verifyReturnsFalseForWrongLengthSignature() {
        Map<String, String> params = Map.of("a", "1");

        assertThat(signer.verify(params, "tooshort")).isFalse();
        assertThat(signer.verify(params, null)).isFalse();
        assertThat(signer.verify(params, "")).isFalse();
    }

    @Test
    void constructorRejectsBlankSecret() {
        assertThatThrownBy(() -> new HmacSha512Signer(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HmacSha512Signer(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HmacSha512Signer("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
