package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.TransactionTimedOutException;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Recognises a cancelled statement, whatever layer translated it.
 * <p>
 * Spring translates PostgreSQL's cancel (SQLState 57014) to {@code DataAccessResourceFailureException}, not to
 * {@link QueryTimeoutException}, so the SQL state and vendor codes of the cause chain are checked as well.
 */
public final class BFQueryTimeouts {

    // PostgreSQL and H2: statement cancelled (query timeout or cancel request)
    private static final String SQL_STATE_QUERY_CANCELED = "57014";
    // MySQL: ER_QUERY_INTERRUPTED (KILL QUERY sent by the driver on timeout)
    private static final int MYSQL_QUERY_INTERRUPTED = 1317;
    // MySQL: ER_QUERY_TIMEOUT (max_execution_time exceeded)
    private static final int MYSQL_QUERY_TIMEOUT = 3024;

    private BFQueryTimeouts() {
    }

    public static boolean isTimeout(Throwable throwable) {
        Map<Throwable, Boolean> seen = new IdentityHashMap<>();
        for (Throwable t = throwable; t != null && seen.put(t, Boolean.TRUE) == null; t = t.getCause()) {
            if (t instanceof QueryTimeoutException
                    || t instanceof TransactionTimedOutException
                    || t instanceof SQLTimeoutException)
                return true;

            if (t instanceof SQLException sqlException && isTimeout(sqlException))
                return true;
        }
        return false;
    }

    private static boolean isTimeout(SQLException e) {
        if (SQL_STATE_QUERY_CANCELED.equals(e.getSQLState()))
            return true;

        // MySQL reports these under SQLState 70100 / HY000; other drivers may reuse the numbers with other states
        String sqlState = e.getSQLState();
        boolean mysqlState = sqlState == null || sqlState.startsWith("70") || sqlState.startsWith("HY");
        return mysqlState && (e.getErrorCode() == MYSQL_QUERY_INTERRUPTED || e.getErrorCode() == MYSQL_QUERY_TIMEOUT);
    }
}
