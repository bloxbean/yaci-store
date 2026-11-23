package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Gives each statement the time left in the Blockfrost request's budget, right before it executes.
 * <p>
 * A smaller timeout set by the caller is kept, and restored after the execution so a reused statement starts from
 * the caller's value. Once the budget is used up, the statement is not sent to the database.
 */
public class BFQueryTimeoutListener implements QueryExecutionListener {

    private static final String CALLER_TIMEOUT = BFQueryTimeoutListener.class.getName() + ".callerTimeout";

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (!BFQueryTimeoutContext.isActive())
            return;

        int remaining = BFQueryTimeoutContext.remainingSeconds();
        Statement statement = execInfo.getStatement();
        try {
            int callerTimeout = statement.getQueryTimeout();
            execInfo.addCustomValue(CALLER_TIMEOUT, callerTimeout);
            statement.setQueryTimeout(callerTimeout > 0 ? Math.min(callerTimeout, remaining) : remaining);
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Could not set the Blockfrost query timeout", e);
        }
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        Integer callerTimeout = execInfo.getCustomValue(CALLER_TIMEOUT, Integer.class);
        if (callerTimeout != null) {
            try {
                execInfo.getStatement().setQueryTimeout(callerTimeout);
            } catch (SQLException e) {
                // The statement or its connection may already be closed after a cancelled execution
            }
        }

        if (!execInfo.isSuccess() && BFQueryTimeouts.isTimeout(execInfo.getThrowable()))
            BFQueryTimeoutContext.markTimedOut();
    }
}
