package com.bloxbean.cardano.yaci.store.snapshot.it;

import com.bloxbean.cardano.yaci.store.snapshot.load.SequenceReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sequence reset after a bulk load that writes explicit key values, against a real PostgreSQL
 * server. Runs only when {@code SNAPSHOT_IT_JDBC_URL} is set. See {@link PostgresSupport}.
 */
@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class SequenceResetIT {

    private String schema;

    @BeforeEach
    void setUp() throws Exception {
        schema = PostgresSupport.schema();
        try (Connection conn = PostgresSupport.connect()) {
            PostgresSupport.resetSchema(conn);
        }
    }

    @Test
    void aTableLoadedWithExplicitKeysContinuesAfterItsLargestKey() throws Exception {
        try (Connection conn = PostgresSupport.connect(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO " + schema + ".rollback (id) SELECT n FROM generate_series(1, 5) n");

            Map<String, Long> reset = new SequenceReset(conn, schema).resetTables(Set.of("rollback"));

            assertThat(reset).containsEntry("rollback_id_seq", 5L);
            assertThat(nextval(st)).isEqualTo(6);
        }
    }

    @Test
    void anEmptyTableStartsAtOne() throws Exception {
        try (Connection conn = PostgresSupport.connect(); Statement st = conn.createStatement()) {
            new SequenceReset(conn, schema).resetAll();

            assertThat(nextval(st)).isEqualTo(1);
        }
    }

    private long nextval(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT nextval('" + schema + ".rollback_id_seq')")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
