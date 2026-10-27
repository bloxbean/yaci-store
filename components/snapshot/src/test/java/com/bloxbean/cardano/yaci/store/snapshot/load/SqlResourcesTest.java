package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SpecException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlResourcesTest {

    @Test
    void acceptsAPlainSelect() {
        SqlResources.validate("t.sql", "SELECT a, b FROM ${files}");
    }

    @Test
    void acceptsALeadingWithClause() {
        SqlResources.validate("t.sql", "WITH x AS (SELECT 1) SELECT * FROM x");
    }

    @Test
    void rejectsWriteStatements() {
        for (String bad : new String[]{
                "INSERT INTO t VALUES (1)",
                "SELECT 1; DROP TABLE t",
                "SELECT * FROM t WHERE x IN (SELECT 1) UNION SELECT * FROM (COPY t TO 'x')",
                "ATTACH 'x' AS y",
                "SELECT * FROM t WHERE 1=1 -- ok\nCREATE TABLE evil (a int)"}) {
            assertThatThrownBy(() -> SqlResources.validate("t.sql", bad))
                    .as("must reject: %s", bad)
                    .isInstanceOf(SpecException.class);
        }
    }

    @Test
    void rejectsMultipleStatements() {
        assertThatThrownBy(() -> SqlResources.validate("t.sql", "SELECT 1; SELECT 2"))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("exactly one statement");
    }

    @Test
    void keywordsInsideIdentifiersAreNotFalsePositives() {
        SqlResources.validate("t.sql",
                "SELECT created_at, dropped_count, inserted_rows FROM ${files}");
    }

    @Test
    void aTransformMayNotScanFilesItself() {
        // The caller binds ${files} to a scan that reads the files' own columns; a transform that
        // called read_parquet itself would let hive directory values replace them again.
        for (String bad : new String[]{"SELECT * FROM read_parquet(${files})",
                "SELECT * FROM PARQUET_SCAN(['x.parquet'])"}) {
            assertThatThrownBy(() -> SqlResources.validate("t.sql", bad))
                    .isInstanceOf(SpecException.class)
                    .hasMessageContaining("reads its inputs through ${files}");
        }
    }

    @Test
    void commentsAreIgnoredWhenDecidingTheLeadingKeyword() {
        SqlResources.validate("t.sql", "-- a leading comment\n-- another\nSELECT 1");
    }

    @Test
    void bindsPlaceholders() {
        String bound = SqlResources.bind("SELECT * FROM ${files} WHERE slot <= ${cutSlot}",
                Map.of("files", "t", "cutSlot", "123"));
        assertThat(bound).isEqualTo("SELECT * FROM t WHERE slot <= 123");
    }

    @Test
    void anUnknownPlaceholderIsAnError() {
        assertThatThrownBy(() -> SqlResources.bind("SELECT ${nope}", Map.of()))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("unknown parameter");
    }

    @Test
    void onlyClasspathResourcesAreAccepted() {
        assertThatThrownBy(() -> SqlResources.read("file:///tmp/x.sql"))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("Only classpath SQL resources");
    }

    @Test
    void builtInAddressUtxoTransformIsPresentAndReadOnly() {
        String sql = SqlResources.read("classpath:/snapshot/sql/address_utxo_v3.sql");
        assertThat(sql).contains("list_sort").contains("${dep.block}").contains("${cutSlot}");
    }
}
