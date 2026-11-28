package com.bloxbean.cardano.yaci.store.common.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Waits a bounded time for database work running on other threads, and ends that work's open transactions when the
 * wait gives up.
 *
 * <p>Each worker calls {@link #trackCurrentTransaction()} inside the transaction it runs in. If
 * {@link #awaitAll(Duration, String, CompletableFuture[])} times out, every connection still tracked is aborted with
 * {@link Connection#abort}. For PostgreSQL that closes the socket without taking any driver lock, so it also works
 * when the worker thread is stuck inside the driver; the server then ends the session, rolls its transaction back
 * and releases its row locks. The caller gets a {@link DbWorkTimeoutException} and fails through its normal error
 * path. Without this, one stuck call held its row locks for good, and every sync session the health check restarted
 * queued behind them until the connection pool ran out.
 *
 * <p>A connection is tracked only until its transaction completes (Spring's {@code afterCompletion}, which runs
 * before the connection goes back to the pool), so a connection already handed to other work is never aborted.
 */
@Slf4j
public class InFlightTransactions {
    private final DataSource dataSource;
    private final Set<Connection> connections = new HashSet<>();
    private boolean gaveUp;

    public InFlightTransactions(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Tracks the connection of the transaction running on the calling thread. Does nothing when no transaction for
     * this tracker's data source is active on the thread.
     *
     * @throws DbWorkTimeoutException if the waiter has already given up, so a worker that starts late does not go on
     *                                writing and its transaction rolls back
     */
    public void trackCurrentTransaction() {
        if (dataSource == null
                || !TransactionSynchronizationManager.isSynchronizationActive()
                || TransactionSynchronizationManager.getResource(dataSource) == null) {
            log.debug("No transaction bound to the data source on this thread, nothing to track");
            return;
        }

        Connection connection = DataSourceUtils.getConnection(dataSource);
        // Only drops the reference count taken above; the transaction keeps the connection
        DataSourceUtils.releaseConnection(connection, dataSource);

        if (!track(connection))
            throw new DbWorkTimeoutException("The wait for this database work was already given up");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                untrack(connection);
            }
        });
    }

    /**
     * Waits until all futures complete, at most {@code timeout}. A null, zero or negative timeout waits without a
     * limit, as before this class existed.
     *
     * @throws DbWorkTimeoutException if the timeout passes; the tracked connections are aborted first
     * @throws CompletionException    if a future failed, with that failure as the cause
     */
    public void awaitAll(Duration timeout, String description, CompletableFuture<?>... futures) {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures);
        try {
            if (timeout == null || timeout.isZero() || timeout.isNegative())
                all.get();
            else
                all.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            int aborted = abortAll();
            throw new DbWorkTimeoutException(String.format(
                    "%s did not finish within %s. Aborted %d database connection(s) it was still using, so their "
                            + "transactions roll back and release their locks", description, timeout, aborted), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            abortAll();
            throw new DbWorkTimeoutException(description + " was interrupted while waiting", e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
    }

    synchronized boolean track(Connection connection) {
        if (gaveUp)
            return false;
        connections.add(connection);
        return true;
    }

    synchronized void untrack(Connection connection) {
        connections.remove(connection);
    }

    synchronized int abortAll() {
        gaveUp = true;
        List<Connection> toAbort = new ArrayList<>(connections);
        connections.clear();

        int aborted = 0;
        for (Connection connection : toAbort) {
            if (abort(connection))
                aborted++;
        }
        return aborted;
    }

    /**
     * Number of connections tracked now, that is, of tracked transactions that have not completed.
     */
    public synchronized int trackedCount() {
        return connections.size();
    }

    private static boolean abort(Connection connection) {
        try {
            // Runs on this thread; the PostgreSQL driver only closes the socket
            connection.abort(Runnable::run);
            log.warn("Aborted database connection {} of work that did not finish in time", connection);
            return true;
        } catch (Exception e) {
            log.warn("Could not abort database connection {}", connection, e);
            return false;
        }
    }
}
