package com.bloxbean.cardano.yaci.store.snapshot.archive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Ordered parallel map over independent archive parts, on a bounded pool of platform threads. */
public final class Parallel {

    @FunctionalInterface
    public interface Task<T, R> {
        R apply(T input) throws Exception;
    }

    private Parallel() {
    }

    /**
     * Applies {@code task} to every input with at most {@code threads} running at once and returns
     * the results in input order. The first failure cancels the tasks not yet started and is rethrown
     * unchanged when it is unchecked or an IOException, wrapped otherwise.
     */
    public static <T, R> List<R> map(List<T> inputs, int threads, Task<T, R> task) {
        int n = Math.max(1, Math.min(threads, inputs.size()));
        ExecutorService pool = Executors.newFixedThreadPool(n, Thread.ofPlatform().name("snapshot-part-", 0).factory());
        try {
            List<Future<R>> futures = new ArrayList<>();
            for (T input : inputs) {
                futures.add(pool.submit(() -> task.apply(input)));
            }
            List<R> results = new ArrayList<>(futures.size());
            for (Future<R> f : futures) {
                try {
                    results.add(f.get());
                } catch (ExecutionException e) {
                    pool.shutdownNow();
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re) throw re;
                    if (cause instanceof Error err) throw err;
                    throw new PartFailure(cause);
                } catch (InterruptedException e) {
                    pool.shutdownNow();
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted", e);
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /** A checked failure of one part's task. */
    public static final class PartFailure extends RuntimeException {
        public PartFailure(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
