package com.bloxbean.cardano.yaci.store.common.executor;

/**
 * Database work on another thread did not finish within its time limit, and its connections were aborted.
 *
 * @see InFlightTransactions
 */
public class DbWorkTimeoutException extends RuntimeException {
    public DbWorkTimeoutException(String message) {
        super(message);
    }

    public DbWorkTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
