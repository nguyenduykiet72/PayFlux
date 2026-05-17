package com.payflux.core.tenant;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Propagates merchant_id across all layers using Java 21 ScopedValue (preview).
 *
 * Usage: {@code TenantContext.callWhere(merchantId, () -> service.doWork())}
 *
 * Unlike ThreadLocal, ScopedValue is immutable and auto-cleanup — no leak risk
 * with virtual threads. The trade-off: all logic must be wrapped in the lambda.
 */
public final class TenantContext {

    public static final ScopedValue<UUID> MERCHANT_ID = ScopedValue.newInstance();

    private TenantContext() {}

    public static UUID get() {
        if (!MERCHANT_ID.isBound()) {
            throw new MissingTenantContextException();
        }
        return MERCHANT_ID.get();
    }

    public static boolean isBound() {
        return MERCHANT_ID.isBound();
    }

    public static <T> T callWhere(UUID merchantId, Callable<T> task) throws Exception {
        return ScopedValue.where(MERCHANT_ID, merchantId).call(task);
    }

    public static void runWhere(UUID merchantId, Runnable task) {
        ScopedValue.where(MERCHANT_ID, merchantId).run(task);
    }
}
