package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.exception.DataAccessException;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Applies the Blockfrost request's remaining query budget to every jOOQ statement it runs.
 * <p>
 * Registered on the shared {@code DSLContext}, but it does nothing on threads without a budget, which includes
 * every indexer thread. An explicit smaller {@code queryTimeout(..)} on a query is kept.
 */
public class BFQueryTimeoutExecuteListener implements ExecuteListener {

    @Override
    public void executeStart(ExecuteContext ctx) {
        if (!BFQueryTimeoutContext.isActive())
            return;

        Statement statement = ctx.statement();
        if (statement == null)
            return;

        int remaining = BFQueryTimeoutContext.remainingSeconds();
        try {
            // jOOQ has already applied the query's own timeout, if any, before executeStart
            int current = statement.getQueryTimeout();
            statement.setQueryTimeout(current > 0 ? Math.min(current, remaining) : remaining);
        } catch (SQLException e) {
            throw new DataAccessException("Could not set the Blockfrost query timeout", e);
        }
    }

    @Override
    public void exception(ExecuteContext ctx) {
        if (!BFQueryTimeoutContext.isActive())
            return;

        if (BFQueryTimeouts.isTimeout(ctx.sqlException()) || BFQueryTimeouts.isTimeout(ctx.exception()))
            BFQueryTimeoutContext.markTimedOut();
    }
}
