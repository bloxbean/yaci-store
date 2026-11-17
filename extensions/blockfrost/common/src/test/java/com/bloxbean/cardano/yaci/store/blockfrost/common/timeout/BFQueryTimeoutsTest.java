package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.transaction.TransactionTimedOutException;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class BFQueryTimeoutsTest {

    @Test
    void springAndJdbcTimeoutTypes() {
        assertThat(BFQueryTimeouts.isTimeout(new QueryTimeoutException("timeout"))).isTrue();
        assertThat(BFQueryTimeouts.isTimeout(new TransactionTimedOutException("deadline reached"))).isTrue();
        assertThat(BFQueryTimeouts.isTimeout(new SQLTimeoutException("timeout"))).isTrue();
    }

    @Test
    void postgresAndH2Cancel_insideTranslatedException() {
        SQLException cancel = new SQLException("ERROR: canceling statement due to user request", "57014");

        assertThat(BFQueryTimeouts.isTimeout(new DataAccessResourceFailureException("jOOQ", cancel))).isTrue();
        assertThat(BFQueryTimeouts.isTimeout(new org.jooq.exception.DataAccessException("SQL [..]", cancel))).isTrue();
    }

    @Test
    void mysqlInterruptedAndMaxExecutionTime() {
        assertThat(BFQueryTimeouts.isTimeout(new SQLException("Query execution was interrupted", "70100", 1317)))
                .isTrue();
        assertThat(BFQueryTimeouts.isTimeout(new SQLException("maximum statement execution time exceeded",
                "HY000", 3024))).isTrue();
    }

    @Test
    void otherDatabaseFailures_areNotTimeouts() {
        assertThat(BFQueryTimeouts.isTimeout(null)).isFalse();
        assertThat(BFQueryTimeouts.isTimeout(new DataAccessResourceFailureException("lock",
                new SQLException("out of shared memory", "53200")))).isFalse();
        assertThat(BFQueryTimeouts.isTimeout(new RecoverableDataAccessException("deadlock",
                new SQLException("deadlock detected", "40P01")))).isFalse();
        // Same vendor number under an unrelated SQL state
        assertThat(BFQueryTimeouts.isTimeout(new SQLException("other", "42000", 1317))).isFalse();
    }

    @Test
    void cyclicCauseChain_terminates() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        assertThat(BFQueryTimeouts.isTimeout(a)).isFalse();
    }
}
