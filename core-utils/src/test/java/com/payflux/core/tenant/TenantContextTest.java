package com.payflux.core.tenant;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TenantContextTest {

    @Test
    void getThrowsWhenNotBound() {
        assertThatThrownBy(TenantContext::get)
            .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void callWhereBindsValue() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID result = TenantContext.callWhere(merchantId, TenantContext::get);
        assertThat(result).isEqualTo(merchantId);
    }

    @Test
    void scopeExitClearsValue() throws Exception {
        UUID merchantId = UUID.randomUUID();
        TenantContext.callWhere(merchantId, () -> null);
        assertThat(TenantContext.isBound()).isFalse();
    }

    @Test
    void runWhereBindsValue() {
        UUID merchantId = UUID.randomUUID();
        TenantContext.runWhere(merchantId, () ->
            assertThat(TenantContext.get()).isEqualTo(merchantId));
    }

    @Test
    void virtualThreadsIsolation() throws Exception {
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();

        var t1 = Thread.ofVirtual().start(() -> {
            try {
                TenantContext.callWhere(m1, () -> {
                    Thread.sleep(100);
                    assertThat(TenantContext.get()).isEqualTo(m1);
                    return null;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        var t2 = Thread.ofVirtual().start(() -> {
            try {
                TenantContext.callWhere(m2, () -> {
                    assertThat(TenantContext.get()).isEqualTo(m2);
                    return null;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        t1.join();
        t2.join();
    }
}
