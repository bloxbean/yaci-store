package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.executor.SyncSessionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Makes sure only the current sync session writes.
 * <p>
 * Every start of the range fetch or chain sync opens a new session, and stop() ends the current one. Callbacks
 * from yaci carry the session they belong to (see BlockFetchService's session listener). A thread that is still
 * inside an old session, for example a listener that hung while a restart started a new session (#1129), must not
 * write: {@link #checkCaller()} throws before the block is committed, so its transaction is rolled back and the
 * cursor is not moved.
 * <p>
 * The checks in the code can pass just before a new session starts. The commit itself is fenced too: see
 * {@link SyncSessionTransactionFence}. A transaction begun by a thread of a session commits only while that session
 * is current, and a session change waits for commits that are already running (see {@link #enterCommit(long)}).
 */
@Component
@Slf4j
public class SyncSessionGuard {
    private final AtomicLong currentSession = new AtomicLong();
    //The mark of the threads working for a session lives in common, so stores can pass it on to their own threads
    //Read lock: held by a commit of a session's transaction, from its session check until the commit is done.
    //Write lock: taken to change the session, so no commit of the old session is in flight when it changes.
    private final ReentrantReadWriteLock commitLock = new ReentrantReadWriteLock();
    //How long a session change waits for commits in flight. Bounded, so a commit that hangs in the database cannot
    //stop the recovery. Not final so tests can shorten it.
    long sessionChangeWaitMs = 30_000;

    /**
     * Opens a new session. All earlier sessions become stale, then this waits for their commits in flight.
     *
     * @throws IllegalStateException if a commit of an earlier session is still running after the wait. The earlier
     *                               sessions are stale all the same, but the caller must not start syncing yet.
     */
    public long newSession() {
        return changeSession(true);
    }

    /**
     * Ends the current session without opening a new one, e.g. when the sync is stopped. Waits for its commits in
     * flight only for a bounded time and never fails, so a stop always goes through. The next start waits strictly
     * (see {@link #awaitCommitsOfOldSessions()}).
     */
    public void endSession() {
        changeSession(false);
    }

    /**
     * Ends the current session and waits until none of its commits is in flight. Commits that come to the fence later
     * see a stale session and are rolled back, so once this returns, nothing of an old session can still be written.
     * Called before a start reads the cursor and rolls back to it.
     *
     * @throws IllegalStateException if a commit is still running after the wait. The start fails, and the restart is
     *                               tried again later.
     */
    public void awaitCommitsOfOldSessions() {
        changeSession(true);
    }

    private long changeSession(boolean strict) {
        Lock writeLock = commitLock.writeLock();
        boolean locked = false;
        try {
            locked = writeLock.tryLock(sessionChangeWaitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            //Always make the old session stale, so its commits that have not reached the fence are rolled back
            long previous = currentSession.get();
            long next = currentSession.incrementAndGet();
            if (!locked) {
                if (strict)
                    throw new IllegalStateException("A commit of sync session " + previous + " is still running after "
                            + sessionChangeWaitMs + " ms. Not starting the sync yet.");
                log.warn("A commit of sync session {} did not finish within {} ms. Ending the session anyway.",
                        previous, sessionChangeWaitMs);
            }
            return next;
        } finally {
            if (locked)
                writeLock.unlock();
        }
    }

    /**
     * Called right before a transaction of the given session commits. Throws {@link StaleSyncSessionException} if the
     * session is no longer current, so the transaction is rolled back. Otherwise returns the release to call once the
     * commit is done: until then, the session cannot change.
     */
    Runnable enterCommit(long session) {
        Lock readLock = commitLock.readLock();
        readLock.lock();
        if (session != currentSession.get()) {
            readLock.unlock();
            throw new StaleSyncSessionException(session, currentSession.get());
        }
        return readLock::unlock;
    }

    /**
     * An executor that runs each task as part of the calling thread's session (taken when this method is called),
     * so the transactions of the task are fenced like the caller's. Without a session, the executor itself.
     */
    public Executor propagating(Executor executor) {
        return SyncSessionContext.propagating(executor);
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
        return SyncSessionContext.enter(session);
    }

    /**
     * Ends the mark set by {@link #enter(long)}, restoring the one before it (null: none).
     */
    public void exit(Long previous) {
        SyncSessionContext.exit(previous);
    }

    public void exit() {
        SyncSessionContext.exit(null);
    }

    /**
     * The session the calling thread works for, or null if it is not marked. Pass it to worker threads that write for
     * the caller, and check it there with {@link #check(Long)}.
     */
    public Long callerSession() {
        return SyncSessionContext.current();
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
        Long session = SyncSessionContext.current();
        return session != null && session != currentSession.get();
    }

    /**
     * Throws {@link StaleSyncSessionException} if the calling thread works for a session that is no longer current.
     * Call it before anything is committed.
     */
    public void checkCaller() {
        Long session = SyncSessionContext.current();
        if (session != null && session != currentSession.get())
            throw new StaleSyncSessionException(session, currentSession.get());
    }

    /**
     * Runs a commit step, such as the cursor write, only for the current session. The session is checked before and
     * after the write, in one transaction (the caller's, if it has one). A thread of an old session can pass the
     * earlier checks and then wait, for example in a commit listener or for a database connection, while a restart
     * starts a new session. The check after the write then rolls the write back. The cursor write is flushed
     * before that check, so the only window left is the commit itself.
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
