package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs a short call to the node with a time limit.
 * <p>
 * yaci 0.4.7 waits for the connection and the handshake with no time limit (it retries the connect, then sleeps in
 * a loop until the handshake is done). A node that accepts the connection but never answers would hold the caller
 * forever: the startup, or a restart that holds the restart lock. Here the call runs on its own thread, and the
 * caller gives up after the limit.
 */
@Slf4j
final class NodeCalls {
    /** Time limit for one call when auto-restart is on. */
    static final long DEFAULT_TIMEOUT_MS = 40_000;

    private NodeCalls() {
    }

    /**
     * With auto-restart on, a call that times out fails the start and the restart tries again later. With it off,
     * nothing would retry, so keep the old behaviour and wait for the connection as long as it takes. (The
     * cursor check still limits the wait for the node's answer, see {@link BlockFinder}.)
     */
    static long timeoutMs(StoreProperties storeProperties) {
        return storeProperties != null && storeProperties.isAutoRestartEnabled() ? DEFAULT_TIMEOUT_MS : 0;
    }

    /**
     * Runs {@code call} on a new thread and waits for it.
     *
     * @param what      what the call does, for messages
     * @param timeoutMs time limit, 0 for none
     * @param cancel    closes the connection the call opened (stops yaci's reconnect); may run more than once
     * @throws NodeNotAnsweringException if the call did not finish in time
     */
    static <T> T call(String what, long timeoutMs, Callable<T> call, Runnable cancel) {
        CompletableFuture<T> result = new CompletableFuture<>();
        // A platform thread, not a virtual one: the caller may be a virtual thread pinned by synchronized methods,
        // and on a small machine a virtual worker could then wait for a free carrier until the timeout.
        Thread worker = Thread.ofPlatform().daemon().name("node-call").start(() -> {
            try {
                result.complete(call.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });

        boolean callerInterrupted = false;
        try {
            return timeoutMs > 0 ? result.get(timeoutMs, TimeUnit.MILLISECONDS) : result.get();
        } catch (TimeoutException e) {
            throw new NodeNotAnsweringException("The node did not answer within " + (timeoutMs / 1000) + " s: " + what);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re)
                throw re;
            if (cause instanceof Error err)
                throw err;
            throw new IllegalStateException(what + " failed", cause);
        } catch (InterruptedException e) {
            callerInterrupted = true;
            throw new IllegalStateException("Interrupted: " + what, e);
        } finally {
            if (!result.isDone()) {
                // Clear the caller's interrupt so the join really waits; restored below.
                callerInterrupted |= Thread.interrupted();
                runQuietly(cancel);     // close the connection and stop yaci's reconnect
                worker.interrupt();     // end yaci's handshake wait loop
                try {
                    worker.join(5000);
                } catch (InterruptedException e) {
                    callerInterrupted = true;
                }
                runQuietly(cancel);     // close anything the worker opened after the first cancel
                if (worker.isAlive())
                    log.warn("{}: the call to the node is still running after it was cancelled", what);
            }
            if (callerInterrupted)
                Thread.currentThread().interrupt();
        }
    }

    private static void runQuietly(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            log.debug("Error while cancelling a call to the node", e);
        }
    }
}
