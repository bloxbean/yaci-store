package com.bloxbean.cardano.yaci.store.common.executor;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Marks the threads that work for a sync session (#1129). Core opens and checks the sessions (SyncSessionGuard),
 * and it rolls back a transaction begun on a marked thread if its session is no longer current when it commits.
 * <p>
 * A thread a store starts on its own (an executor, a parallel stream) is not marked. Code that writes from such a
 * thread for a block passes the mark on with the {@code propagating} methods: the session of the calling thread is
 * taken when the method is called and set on the worker while the task runs.
 */
public final class SyncSessionContext {
    private static final ThreadLocal<Long> SESSION = new ThreadLocal<>();

    private SyncSessionContext() {
    }

    /**
     * The session the calling thread works for, or null.
     */
    public static Long current() {
        return SESSION.get();
    }

    /**
     * Marks the calling thread as working for the session. Returns the previous mark, to restore with
     * {@link #exit(Long)}.
     */
    public static Long enter(long session) {
        Long previous = SESSION.get();
        SESSION.set(session);
        return previous;
    }

    public static void exit(Long previous) {
        if (previous == null)
            SESSION.remove();
        else
            SESSION.set(previous);
    }

    public static Executor propagating(Executor executor) {
        Long session = current();
        if (session == null)
            return executor;
        return task -> executor.execute(() -> runAs(session, task));
    }

    public static Runnable propagating(Runnable task) {
        Long session = current();
        if (session == null)
            return task;
        return () -> runAs(session, task);
    }

    public static <T> Supplier<T> propagating(Supplier<T> supplier) {
        Long session = current();
        if (session == null)
            return supplier;
        return () -> {
            Long previous = enter(session);
            try {
                return supplier.get();
            } finally {
                exit(previous);
            }
        };
    }

    public static <T> Consumer<T> propagating(Consumer<T> consumer) {
        Long session = current();
        if (session == null)
            return consumer;
        return value -> runAs(session, () -> consumer.accept(value));
    }

    private static void runAs(long session, Runnable task) {
        Long previous = enter(session);
        try {
            task.run();
        } finally {
            exit(previous);
        }
    }
}
