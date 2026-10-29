package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UnspentUtxoTableService} on PostgreSQL with the module's own postgresql migrations.
 * The H2 test covers the behaviour; this one checks that the jOOQ-rendered SQL runs on the
 * dialect production uses (jsonb amounts, the row-value IN of the delete) and that the commit
 * delete is driven from tx_input rather than scanning the table.
 */
@Testcontainers(disabledWithoutDocker = true)
class UnspentUtxoTablePostgresIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    private static JdbcTemplate jdbc;
    private static UnspentUtxoTableService service;

    @BeforeAll
    static void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds)
                .locations("filesystem:src/main/resources/db/store/postgresql")
                .load().migrate();
        jdbc = new JdbcTemplate(ds);
        DSLContext dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        service = new UnspentUtxoTableService(dsl, new DataSourceTransactionManager(ds));
    }

    @AfterEach
    void clean() {
        jdbc.update("delete from address_utxo_unspent");
        jdbc.update("delete from address_utxo");
        jdbc.update("delete from tx_input");
    }

    private static String hash(int n) {
        return String.format("%064x", n);
    }

    private static void output(String txHash, int index, long slot) {
        jdbc.update("""
                insert into address_utxo (tx_hash, output_index, slot, block, block_time, owner_addr, lovelace_amount, amounts, tx_index)
                values (?, ?, ?, ?, ?, ?, 1000000, cast(? as jsonb), ?)""",
                txHash, index, slot, slot / 10, 1_600_000L + slot, "addr_test1" + txHash.substring(0, 8),
                "[{\"unit\": \"lovelace\", \"quantity\": 1000000}, {\"unit\": \"aa\", \"quantity\": 7}]", index);
    }

    private static void spend(String txHash, int index, long atSlot) {
        jdbc.update("insert into tx_input (tx_hash, output_index, spent_at_slot, spent_at_block, spent_tx_hash) values (?, ?, ?, ?, ?)",
                txHash, index, atSlot, atSlot / 10, "spender" + atSlot);
    }

    private static List<String> unspentKeys() {
        return jdbc.queryForList("select tx_hash || '#' || output_index from address_utxo_unspent order by 1", String.class);
    }

    private static List<String> expectedKeys() {
        return jdbc.queryForList("""
                select a.tx_hash || '#' || a.output_index from address_utxo a
                where not exists (select 1 from tx_input i where i.tx_hash = a.tx_hash and i.output_index = a.output_index)
                order by 1""", String.class);
    }

    @Test
    void commitRollbackBackfillAndReconcileKeepTheTableEqualToTheAntiJoin() {
        spend(hash(1), 0, 110);
        output(hash(1), 0, 100);
        output(hash(1), 1, 100);
        service.applyCommit(100, 110);
        assertThat(unspentKeys()).containsExactly(hash(1) + "#1").isEqualTo(expectedKeys());

        spend(hash(1), 1, 200);
        output(hash(2), 0, 200);
        service.applyCommit(200, 200);
        assertThat(unspentKeys()).containsExactly(hash(2) + "#0").isEqualTo(expectedKeys());

        service.rollbackTo(150);
        jdbc.update("delete from address_utxo where slot > 150");
        jdbc.update("delete from tx_input where spent_at_slot > 150");
        assertThat(unspentKeys()).containsExactly(hash(1) + "#1").isEqualTo(expectedKeys());

        jdbc.update("delete from address_utxo_unspent");
        for (int i = 0; i < 10; i++) {
            output(hash(50 + i), 0, 300L + i);
        }
        assertThat(service.backfill(0, 400, 3)).isEqualTo(11);
        spend(hash(52), 0, 500);
        assertThat(service.reconcileSpentSince(450)).isEqualTo(1);
        assertThat(unspentKeys()).hasSize(10).isEqualTo(expectedKeys());
        assertThat(jdbc.queryForObject("select amounts::text from address_utxo_unspent where tx_hash = ?", String.class, hash(50)))
                .contains("\"unit\": \"aa\"");
    }

    @Test
    void theCommitDeleteIsDrivenFromTheBatchsSpendsNotAScanOfTheTable() {
        for (int i = 0; i < 2000; i++) {
            output(hash(1000 + i), 0, i);
        }
        service.backfill(0, 2000, 500);
        spend(hash(1500), 0, 3000);
        jdbc.execute("analyze address_utxo_unspent");
        jdbc.execute("analyze tx_input");

        List<String> plan = jdbc.queryForList("""
                explain delete from address_utxo_unspent
                where (tx_hash, output_index) in
                      (select tx_hash, output_index from tx_input where spent_at_slot between 3000 and 3000)""", String.class);

        assertThat(String.join("\n", plan)).doesNotContain("Seq Scan on address_utxo_unspent");
        assertThat(service.applyCommit(3000, 3000)[1]).isEqualTo(1);
    }
}
