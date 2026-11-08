package com.bloxbean.cardano.yaci.store.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public Long enter(long session) {
        Long previous = callerSession.get();
        callerSession.set(session);
        return previous;
    }

    /**
     * Ends the mark set by {@link #enter(long)}, restoring the one before it (null: none).
     */
    public void exit(Long previous) {
        if (previous == null)
            callerSession.remove();
        else
            callerSession.set(previous);
    }

    public void exit() {
        callerSession.remove();
    }

    /**
     * The session the calling thread works for, or null if it is not marked. Pass it to worker threads that write for
     * the caller, and check it there with {@link #check(Long)}.
     */
    public Long callerSession() {
        return callerSession.get();
    }

    /**
     * Throws {@link StaleSyncSessionException} if the given session (from {@link #callerSession()}) is no longer
     * current. A null session is never stale.
     */
    public void check(Long session) {
        if (session != null && session != currentSession.get())
            throw new StaleSyncSessionException(session, currentSession.get());
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

    /**
     * Runs a commit step, such as the cursor write, only for the current session. The session is checked before and
     * after the write, in one transaction (the caller's, if it has one). A thread of an old session can pass the
     * earlier checks and then wait, for example in a commit listener or for a database connection, while a restart
     * starts a new session. The check after the write then rolls the write back.
     */
    @Transactional
    public void writeIfCurrent(Runnable write) {
        checkCaller();
        write.run();
        checkCaller();
    }

    public static class StaleSyncSessionException extends RuntimeException {
        public StaleSyncSessionException(long session, long current) {
            super("Sync session " + session + " is no longer current (current: " + current + "). Not writing.");
        }
    }
}
