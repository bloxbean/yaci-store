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

    @Test
    void aSessionChangeMakesTheOldSessionStaleBeforeItWaitsForItsCommits() throws Exception {
        //#1332 review (ccf461e62) finding 2: while a commit of the old session hangs, the stop must end the session at
        //once, and a commit of the old session that comes to the fence during the wait must be refused, not queued
        guard.sessionChangeWaitMs = 2_000;
        long old = guard.newSession();
        Runnable hanging = guard.enterCommit(old);
        Thread stop = new Thread(guard::endSession);
        stop.start();
        try {
            long deadline = System.currentTimeMillis() + 500;
            while (guard.isCurrent(old)) {
                if (System.currentTimeMillis() > deadline)
                    throw new AssertionError("the old session is still current while the stop waits");
                Thread.sleep(5);
            }
            assertThat(stop.isAlive()).isTrue(); //still waiting for the hanging commit

            var late = java.util.concurrent.CompletableFuture.supplyAsync(() -> guard.enterCommit(old));
            assertThatThrownBy(() -> late.get(500, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .hasCauseInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            hanging.run();
            stop.join(5_000);
        }
        assertThat(stop.isAlive()).isFalse();
    }
}
