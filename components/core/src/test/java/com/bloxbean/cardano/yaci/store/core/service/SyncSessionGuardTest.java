package com.bloxbean.cardano.yaci.store.core.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncSessionGuardTest {
    private final SyncSessionGuard guard = new SyncSessionGuard();

    @Test
    void threadOutsideAnySessionIsNeverStale() {
        guard.newSession();
        guard.newSession();

        assertThat(guard.isCallerStale()).isFalse();
        guard.checkCaller();
    }

    @Test
    void threadOfTheCurrentSessionMayWrite() {
        long session = guard.newSession();
        guard.enter(session);
        try {
            assertThat(guard.isCallerStale()).isFalse();
            guard.checkCaller();
        } finally {
            guard.exit();
        }
    }

    @Test
    void threadOfAnOldSessionMayNotWrite() {
        long session = guard.newSession();
        guard.enter(session);
        try {
            guard.endSession();

            assertThat(guard.isCallerStale()).isTrue();
            assertThatThrownBy(guard::checkCaller).isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }
        assertThat(guard.isCallerStale()).isFalse();
    }

    @Test
    void aSessionOpensItsSuccessorOnlyWhileItIsCurrent() {
        //#1332 review finding 5: a stop just before the range session hands over to chain sync wins
        long range = guard.newSession();
        guard.endSession();
        long afterStop = guard.currentSession();

        assertThatThrownBy(() -> guard.newSessionReplacing(range))
                .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        assertThat(guard.currentSession()).isEqualTo(afterStop);

        assertThat(guard.newSessionReplacing(afterStop)).isEqualTo(afterStop + 1);
        assertThat(guard.newSessionReplacing(null)).isEqualTo(afterStop + 2);
    }
}
