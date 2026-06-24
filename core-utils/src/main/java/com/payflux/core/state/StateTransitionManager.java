package com.payflux.core.state;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * Table-driven state machine for payment lifecycle.
 * Each state maps to its set of valid next states.
 *
 * EnumMap uses array indexing (O(1) by ordinal), faster than HashMap.
 * Adding a new state = adding one put() line + enum constant.
 */
public final class StateTransitionManager {

    private static final EnumMap<PaymentState, Set<PaymentState>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(PaymentState.class);
        ALLOWED.put(PaymentState.PENDING,    EnumSet.of(PaymentState.AUTHORIZED, PaymentState.CAPTURED, PaymentState.CANCELLED));
        ALLOWED.put(PaymentState.AUTHORIZED, EnumSet.of(PaymentState.CAPTURED, PaymentState.CANCELLED, PaymentState.FAILED));
        ALLOWED.put(PaymentState.CAPTURED,   EnumSet.of(PaymentState.REFUNDED));
        ALLOWED.put(PaymentState.FAILED,     EnumSet.noneOf(PaymentState.class));
        ALLOWED.put(PaymentState.REFUNDED,   EnumSet.noneOf(PaymentState.class));
        ALLOWED.put(PaymentState.CANCELLED,  EnumSet.noneOf(PaymentState.class));
    }

    private StateTransitionManager() {}

    public static boolean canTransition(PaymentState from, PaymentState to) {
        return ALLOWED.get(from).contains(to);
    }

    public static void assertCanTransition(PaymentState from, PaymentState to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateTransitionException(from, to);
        }
    }
}
