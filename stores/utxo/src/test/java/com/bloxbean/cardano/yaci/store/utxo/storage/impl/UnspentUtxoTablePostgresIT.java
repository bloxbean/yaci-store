package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
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

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The address_utxo_unspent writes of {@link UtxoStorageImpl} on PostgreSQL with the module's own
 * postgresql migrations. The H2 test covers the behaviour; this one checks that the jOOQ-rendered
 * SQL runs on the dialect production uses (jsonb amounts, the upsert, the rollback restore).
 *
 * <p>The JPA repositories are mocks, so the two deletes they own in a rollback (address_utxo and
 * tx_input after the point) are done here in plain SQL, in the order UtxoRollbackProcessor runs them.
 */
@Testcontainers(disabledWithoutDocker = true)
class UnspentUtxoTablePostgresIT {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");

    private static JdbcTemplate jdbc;
    private static UtxoStorageImpl storage;

    @BeforeAll
    static void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds)
                .locations("filesystem:src/main/resources/db/store/postgresql")
                .load().migrate();
        jdbc = new JdbcTemplate(ds);
        DSLContext dsl = DSL.using(new TransactionAwareDataSourceProxy(ds), SQLDialect.POSTGRES);
        var transactionManager = new DataSourceTransactionManager(ds);
        storage = new UtxoStorageImpl(mock(UtxoRepository.class), mock(TxInputRepository.class), dsl,
                new UtxoCache(), transactionManager,
                new UnspentUtxoTable(UtxoStoreProperties.builder().unspentTableEnabled(true).build(), dsl, transactionManager));
    }

    @AfterEach
    void clean() {
        commit();
        jdbc.update("delete from address_utxo_unspent");
        jdbc.update("delete from address_utxo");
        jdbc.update("delete from tx_input");
    }

    private static String hash(int n) {
        return String.format("%064x", n);
    }

    private static AddressUtxo output(String txHash, int index, long slot) {
        return AddressUtxo.builder()
                .txHash(txHash).outputIndex(index).slot(slot).blockNumber(slot / 10).blockTime(1_600_000L + slot)
                .txIndex(index).ownerAddr("addr_test1" + txHash.substring(0, 8))
                .lovelaceAmount(BigInteger.valueOf(1_000_000L))
                .amounts(List.of(
                        Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(1_000_000L)).build(),
                        Amt.builder().unit("aa").quantity(BigInteger.valueOf(7)).build()))
                .build();
    }

    private static TxInput spend(String txHash, int index, long atSlot) {
        return TxInput.builder().txHash(txHash).outputIndex(index)
                .spentAtSlot(atSlot).spentAtBlock(atSlot / 10).spentTxHash("spender" + atSlot).build();
    }

    private static void commit() {
        storage.handleCommit(new CommitEvent<>(EventMetadata.builder().build(), List.of()));
    }

    private static void rollbackTo(long slot) {
        storage.deleteUnspentBySlotGreaterThan(slot);
        jdbc.update("delete from address_utxo where slot > ?", slot);
        storage.deleteSpentBySlotGreaterThan(slot);
        jdbc.update("delete from tx_input where spent_at_slot > ?", slot);
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
    void writesCommitsAndRollbacksKeepTheTableEqualToTheAntiJoin() {
        storage.saveSpent(List.of(spend(hash(1), 0, 110)));
        storage.saveUnspent(List.of(output(hash(1), 0, 100), output(hash(1), 1, 100)));
        commit();
        assertThat(unspentKeys()).containsExactly(hash(1) + "#1").isEqualTo(expectedKeys());

        storage.saveSpent(List.of(spend(hash(1), 1, 200)));
        storage.saveUnspent(List.of(output(hash(2), 0, 200)));
        commit();
        assertThat(unspentKeys()).containsExactly(hash(2) + "#0").isEqualTo(expectedKeys());

        rollbackTo(150);
        assertThat(unspentKeys()).containsExactly(hash(1) + "#1").isEqualTo(expectedKeys());

        assertThat(jdbc.queryForObject("select amounts::text from address_utxo_unspent", String.class))
                .contains("\"unit\": \"aa\"");
    }

    @Test
    void anUpsertOverwritesTheRow() {
        storage.saveUnspent(List.of(output(hash(3), 0, 100)));
        AddressUtxo resolved = output(hash(3), 0, 100);
        resolved.setOwnerStakeAddr("stake_test1resolved");
        storage.saveUnspent(List.of(resolved));
        commit();

        assertThat(jdbc.queryForObject("select owner_stake_addr from address_utxo_unspent", String.class))
                .isEqualTo("stake_test1resolved");
    }
}
