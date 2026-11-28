package com.bloxbean.cardano.yaci.store.dbutils.index.service;

import com.bloxbean.cardano.yaci.store.dbutils.index.util.DatabaseUtils;
import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class PoolBlockIndexTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void populateBlocks() {
        var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE block (hash varchar(64) PRIMARY KEY, number bigint, slot_leader varchar(56), epoch integer, slot bigint)");
        jdbc.execute("INSERT INTO block SELECT md5(n::text), n, CASE WHEN n % 500 = 1 AND n <= 500000 "
                + "THEN 'target_pool' ELSE 'other_pool_' || (n % 1000)::text END, n / 20000, n * 20 "
                + "FROM generate_series(1, 600000) n");
        jdbc.execute("CREATE INDEX idx_block_number ON block(number)");
        jdbc.execute("CREATE INDEX idx_block_slot_leader ON block(slot_leader)");
        jdbc.execute("ANALYZE block");

        var databaseUtils = new DatabaseUtils(jdbc, DSL.using(dataSource, SQLDialect.POSTGRES));
        var blockIndexes = new IndexLoader().loadIndexes("blockfrost-index.yml").stream()
                .filter(definition -> "block".equals(definition.getTable())).toList();
        var result = new IndexService(jdbc, databaseUtils).applyIndexes(blockIndexes);
        assertThat(result.getSecond()).isEmpty();
    }

    @Test
    void descendingPoolPageUsesAnOrderedPoolIndexInsteadOfFilteringTheChain() {
        assertOrderedPoolScan("DESC");

        var numbers = jdbc.queryForList("SELECT number FROM block WHERE slot_leader = 'target_pool' "
                + "ORDER BY number DESC LIMIT 100 OFFSET 100", Long.class);
        assertThat(numbers).hasSize(100).isSortedAccordingTo((left, right) -> Long.compare(right, left));
        assertThat(numbers.getFirst()).isEqualTo(449_501);
        assertThat(numbers.getLast()).isEqualTo(400_001);
    }

    @Test
    void ascendingPoolPageUsesTheSameIndexInTheOtherDirection() {
        assertOrderedPoolScan("ASC");

        var numbers = jdbc.queryForList("SELECT number FROM block WHERE slot_leader = 'target_pool' "
                + "ORDER BY number ASC LIMIT 100 OFFSET 100", Long.class);
        assertThat(numbers).hasSize(100).isSorted();
        assertThat(numbers.getFirst()).isEqualTo(50_001);
        assertThat(numbers.getLast()).isEqualTo(99_501);
    }

    private void assertOrderedPoolScan(String order) {
        var plan = String.join("\n", jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS) "
                + "SELECT hash FROM block WHERE slot_leader = 'target_pool' ORDER BY number "
                + order + " LIMIT 100", String.class));
        assertThat(plan).contains("using idx_block_slot_leader_number")
                .contains("Index Cond:").doesNotContain("Sort", "Rows Removed by Filter");
    }
}
