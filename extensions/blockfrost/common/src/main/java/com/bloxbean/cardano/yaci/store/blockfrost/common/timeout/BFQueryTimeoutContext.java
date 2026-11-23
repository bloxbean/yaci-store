package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;

/**
 * Query time budget of the Blockfrost request served by the current thread.
 * <p>
 * The budget is only set on threads serving a Blockfrost controller (see {@link BFQueryTimeoutInterceptor}).
 * Indexer threads never have one, so the statement timeouts derived from it never apply to sync or rollback.
 */
public final class BFQueryTimeoutContext {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private BFQueryTimeoutContext() {
    }

    public static void begin(Duration timeout) {
        CURRENT.set(new State(System.nanoTime() + timeout.toNanos()));
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    /**
     * Seconds left in the budget, rounded up, for {@link java.sql.Statement#setQueryTimeout(int)}.
     * Throws a {@link QueryTimeoutException} once the budget is used up, so no new statement is started.
     */
    public static int remainingSeconds() {
        State state = CURRENT.get();
        if (state == null)
            throw new IllegalStateException("No Blockfrost query budget on this thread");

        long remainingNanos = state.deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
            state.timedOut = true;
            throw new QueryTimeoutException("Blockfrost request query time budget exhausted");
        }

        return (int) Math.min(Integer.MAX_VALUE, (remainingNanos + 999_999_999L) / 1_000_000_000L);
    }

    public static void markTimedOut() {
        State state = CURRENT.get();
        if (state != null)
            state.timedOut = true;
    }

    /**
     * True when a query of the current request timed out, even if the caller caught the exception.
     */
    public static boolean isTimedOut() {
        State state = CURRENT.get();
        return state != null && state.timedOut;
    }

    private static final class State {
        private final long deadlineNanos;
        private boolean timedOut;

        private State(long deadlineNanos) {
            this.deadlineNanos = deadlineNanos;
        }
    }
}
