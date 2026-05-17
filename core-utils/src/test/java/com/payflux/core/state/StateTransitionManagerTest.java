package com.payflux.core.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class StateTransitionManagerTest {

    @ParameterizedTest
    @EnumSource(PaymentState.class)
    void allStatesHaveEntry(PaymentState state) {
        assertThatNoException().isThrownBy(() ->
            StateTransitionManager.canTransition(state, PaymentState.PENDING));
    }

    @Test
    void pendingToAuthorizedAllowed() {
        assertThat(StateTransitionManager.canTransition(
            PaymentState.PENDING, PaymentState.AUTHORIZED)).isTrue();
    }

    @Test
    void pendingToFailedAllowed() {
        assertThat(StateTransitionManager.canTransition(
            PaymentState.PENDING, PaymentState.FAILED)).isTrue();
    }

    @Test
    void authorizedToCapturedAllowed() {
        assertThat(StateTransitionManager.canTransition(
            PaymentState.AUTHORIZED, PaymentState.CAPTURED)).isTrue();
    }

    @Test
    void capturedToRefundedAllowed() {
        assertThat(StateTransitionManager.canTransition(
            PaymentState.CAPTURED, PaymentState.REFUNDED)).isTrue();
    }

    @Test
    void capturedToPendingForbidden() {
        assertThat(StateTransitionManager.canTransition(
            PaymentState.CAPTURED, PaymentState.PENDING)).isFalse();
    }

    @Test
    void assertCanTransitionThrowsOnInvalid() {
        assertThatThrownBy(() ->
            StateTransitionManager.assertCanTransition(PaymentState.CAPTURED, PaymentState.PENDING))
            .isInstanceOf(IllegalStateTransitionException.class)
            .hasMessageContaining("CAPTURED")
            .hasMessageContaining("PENDING");
    }

    @Test
    void terminalStatesHaveNoTransitions() {
        Set<PaymentState> terminals = EnumSet.of(
            PaymentState.FAILED, PaymentState.REFUNDED, PaymentState.CANCELLED);
        for (PaymentState terminal : terminals) {
            for (PaymentState target : PaymentState.values()) {
                assertThat(StateTransitionManager.canTransition(terminal, target))
                    .as("%s should not transition to %s", terminal, target)
                    .isFalse();
            }
        }
    }
}
