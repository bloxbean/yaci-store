package com.bloxbean.cardano.yaci.store.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Makes sure only the current sync session writes.
 * <p>
 * Every start of the range fetch or chain sync opens a new session, and stop() ends the current one. Callbacks
 * from yaci carry the session they belong to (see BlockFetchService's session listener). A thread that is still
 * inside an old session, for example a listener that hung while a restart started a new session (#1129), must not
 * write: {@link #checkCaller()} throws before the block is committed, so its transaction is rolled back and the
 * cursor is not moved.
 */
@Component
@Slf4j
public class SyncSessionGuard {
    private final AtomicLong currentSession = new AtomicLong();
    private final ThreadLocal<Long> callerSession = new ThreadLocal<>();

    /**
     * Opens a new session. All earlier sessions become stale.
     */
    public long newSession() {
        return currentSession.incrementAndGet();
    }

    /**
     * Ends the current session without opening a new one, e.g. when the sync is stopped.
     */
    public void endSession() {
        currentSession.incrementAndGet();
    }

    public long currentSession() {
        return currentSession.get();
    }

    public boolean isCurrent(long session) {
        return session == currentSession.get();
    }

    /**
     * Marks the calling thread as working for the given session, until {@link #exit()}.
     */
    public void enter(long session) {
        callerSession.set(session);
    }

    public void exit() {
        callerSession.remove();
    }

    /**
     * True if the calling thread works for a session that is no longer current. A thread outside any session
     * (startup, tests, a restart) is never stale.
     */
    public boolean isCallerStale() {
        Long session = callerSession.get();
        return session != null && session != currentSession.get();
    }

    /**
     * Throws {@link StaleSyncSessionException} if the calling thread works for a session that is no longer current.
     * Call it before anything is committed.
     */
    public void checkCaller() {
        Long session = callerSession.get();
        if (session != null && session != currentSession.get())
            throw new StaleSyncSessionException(session, currentSession.get());
    }

    public static class StaleSyncSessionException extends RuntimeException {
        public StaleSyncSessionException(long session, long current) {
            super("Sync session " + session + " is no longer current (current: " + current + "). Not writing.");
        }
    }
}
