package com.bloxbean.cardano.yaci.store.dbutils.utxo;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The admin-cli backfill against the utxo store's own H2 schema. */
class UnspentUtxoBackfillServiceTest {

    private JdbcTemplate jdbc;
    private UnspentUtxoBackfillService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(ds)
                .locations("filesystem:../../stores/utxo/src/main/resources/db/store/h2")
                .load().migrate();
        jdbc = new JdbcTemplate(ds);
        service = new UnspentUtxoBackfillService(jdbc);
    }

    private void output(int n, long slot) {
        jdbc.update("insert into address_utxo (tx_hash, output_index, slot, owner_addr, lovelace_amount) values (?, 0, ?, ?, 1)",
                String.format("%064x", n), slot, "addr" + n);
    }

    private void spend(int n, long slot) {
        jdbc.update("insert into tx_input (tx_hash, output_index, spent_at_slot) values (?, 0, ?)", String.format("%064x", n), slot);
    }

    private List<String> unspent() {
        return jdbc.queryForList("select owner_addr from address_utxo_unspent order by owner_addr", String.class);
    }

    @Test
    void copiesTheUnspentOutputsUpToTheTipInChunksThenReconcilesSpendsMadeDuringTheFill() {
        for (int i = 0; i < 10; i++) {
            output(i, 10L * i);
        }
        spend(3, 95);

        long tip = service.tipSlot();
        long copied = service.backfill(0, tip, 25);
        spend(5, 200);                              // the indexer spends one while the fill ran
        long removed = service.reconcileSpentAfter(tip);

        assertThat(tip).isEqualTo(90);
        assertThat(copied).isEqualTo(9);
        assertThat(removed).isEqualTo(1);
        assertThat(unspent()).containsExactly("addr0", "addr1", "addr2", "addr4", "addr6", "addr7", "addr8", "addr9");
    }

    @Test
    void isSafeToRepeatAndToResume() {
        for (int i = 0; i < 6; i++) {
            output(i, 10L * i);
        }
        service.backfill(0, 20, 5);
        assertThat(service.backfill(0, 50, 5)).isEqualTo(3);
        assertThat(service.backfill(0, 50, 5)).isZero();
        assertThat(unspent()).hasSize(6);
    }

    @Test
    void anEmptyStoreHasNoTip() {
        assertThat(service.tipSlot()).isEqualTo(-1);
    }
}
