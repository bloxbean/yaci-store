package com.bloxbean.cardano.yaci.store.snapshot.convert;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterRegistryTest {

    private final ConverterRegistry registry = new ConverterRegistry();

    @Test
    void unknownConverterNamesAreRejected() {
        assertThatThrownBy(() -> registry.get("make-it-nice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown converter");
    }

    @Test
    void timestampConverterRejectsANonTimestampSource() {
        ValueConverter c = registry.get("timestamp-to-epoch-seconds");
        assertThat(c.reject("timestamptz", "bigint")).isNull();
        assertThat(c.reject("varchar", "bigint")).contains("expects a timestamp source");
    }

    @Test
    void jsonbConverterRejectsANonJsonbTarget() {
        ValueConverter c = registry.get("varchar-to-jsonb");
        assertThat(c.reject("varchar", "jsonb")).isNull();
        assertThat(c.reject("varchar", "text")).contains("targets a jsonb column");
    }

    @Test
    void checkedNarrowingRaisesOnOverflowInDuckDb() throws SQLException {
        String sql = registry.get("to-smallint").toSql("v");
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT " + sql + " FROM (SELECT 42::BIGINT AS v)")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(42);
            }
            assertThatThrownBy(() -> st.executeQuery(
                    "SELECT " + sql + " FROM (SELECT 99999::BIGINT AS v)"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void uuidV5SqlMatchesTheJavaReferenceImplementation() throws SQLException {
        UuidV5Converter converter = (UuidV5Converter) registry.get("uuid-v5:tx_hash,idx");
        String sql = converter.toSql(null);

        String txHash = "a3f1c2" + "0".repeat(58);
        int idx = 7;

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT " + sql
                     + " FROM (SELECT '" + txHash + "' AS tx_hash, " + idx + " AS idx)")) {
            assertThat(rs.next()).isTrue();
            String fromSql = rs.getString(1);

            UUID fromJava = UuidV5Converter.compute(UuidV5Converter.keyOf(txHash, String.valueOf(idx)));
            assertThat(fromSql).isEqualTo(fromJava.toString());
            assertThat(UUID.fromString(fromSql).version()).isEqualTo(5);
            assertThat(UUID.fromString(fromSql).variant()).isEqualTo(2);
        }
    }

    @Test
    void uuidV5IsDeterministicAndKeySensitive() {
        UUID a = UuidV5Converter.compute(UuidV5Converter.keyOf("hash", "1"));
        UUID b = UuidV5Converter.compute(UuidV5Converter.keyOf("hash", "1"));
        UUID c = UuidV5Converter.compute(UuidV5Converter.keyOf("hash", "2"));
        assertThat(a).isEqualTo(b).isNotEqualTo(c);
    }

    @Test
    void uuidV5RejectsANonUuidTarget() {
        assertThat(registry.get("uuid-v5:tx_hash").reject("varchar", "uuid")).isNull();
        assertThat(registry.get("uuid-v5:tx_hash").reject("varchar", "text"))
                .contains("targets a uuid column");
    }

    @Test
    void uuidV5KeyColumnsAreIdentifierValidated() {
        assertThatThrownBy(() -> registry.get("uuid-v5:tx_hash; drop table x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SQL identifier");
    }

    @Test
    void timestampConversionProducesUnixSecondsInDuckDb() throws SQLException {
        String sql = registry.get("timestamp-to-epoch-seconds").toSql("t");
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT " + sql
                     + " FROM (SELECT TIMESTAMPTZ '2024-01-15 12:00:00+00' AS t)")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isEqualTo(1705320000L);
        }
    }
}
