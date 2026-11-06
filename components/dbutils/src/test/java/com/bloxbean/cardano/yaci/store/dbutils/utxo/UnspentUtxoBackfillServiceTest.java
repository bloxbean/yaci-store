package com.bloxbean.cardano.yaci.store.dbutils.utxo;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The admin-cli build of address_utxo_unspent against the utxo store's own H2 schema. */
class UnspentUtxoBackfillServiceTest {

    private JdbcTemplate jdbc;
    private UnspentUtxoBackfillService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(ds)
                .locations("filesystem:../../stores/utxo/src/main/resources/db/store/h2",
                        "filesystem:../../components/core/src/main/resources/db/store/h2")   // cursor_, store_state
                .load().migrate();
        jdbc = new JdbcTemplate(ds);
        service = new UnspentUtxoBackfillService(jdbc);
    }

    /** A tx hash whose first hex digit is n % 16, so the outputs fall into every tx_hash slice. */
    private static String hash(int n) {
        return Integer.toHexString(n % 16) + String.format("%063x", n);
    }

    private void output(int n, int index, long slot) {
        jdbc.update("insert into address_utxo (tx_hash, output_index, slot, owner_addr, lovelace_amount, inline_datum) values (?, ?, ?, ?, ?, ?)",
                hash(n), index, slot, "addr" + n + "#" + index, n, "datum" + n);
    }

    private void spend(int n, int index, long slot) {
        jdbc.update("insert into tx_input (tx_hash, output_index, spent_at_slot) values (?, ?, ?)", hash(n), index, slot);
    }

    private List<String> unspent() {
        return jdbc.queryForList("select tx_hash || '#' || output_index from address_utxo_unspent order by 1", String.class);
    }

    /** The definition the table must equal: address_utxo minus tx_input. */
    private List<String> expected() {
        return jdbc.queryForList("""
                select a.tx_hash || '#' || a.output_index from address_utxo a
                where not exists (select 1 from tx_input i where i.tx_hash = a.tx_hash and i.output_index = a.output_index)
                order by 1""", String.class);
    }

    @Test
    void buildsTheTableFromAddressUtxoMinusTxInput() {
        for (int n = 0; n < 40; n++) {
            output(n, n % 3, n == 0 ? -1 : 10L * n);   // n = 0 is a genesis output
            if (n % 4 == 1)
                spend(n, n % 3, 10L * n + 5);
        }
        output(7, 250, 70);                             // a sparse high output_index

        long copied = service.rebuild(0);

        assertThat(copied).isEqualTo(31);
        assertThat(unspent()).hasSize(31).isEqualTo(expected()).contains(hash(0) + "#0", hash(7) + "#250");
        assertThat(jdbc.queryForMap("select slot, owner_addr, lovelace_amount, inline_datum from address_utxo_unspent where tx_hash = ?", hash(6)))
                .containsEntry("SLOT", 60L).containsEntry("OWNER_ADDR", "addr6#0").containsEntry("INLINE_DATUM", "datum6");
    }

    @Test
    void replacesWhateverTheTableHeldBefore() {
        output(1, 0, 10);
        output(2, 0, 20);
        spend(1, 0, 30);
        jdbc.update("insert into address_utxo_unspent (tx_hash, output_index, slot) values (?, 0, 10)", hash(1));   // stale: spent
        jdbc.update("insert into address_utxo_unspent (tx_hash, output_index, slot) values (?, 0, 99)", hash(99));  // stale: not in address_utxo

        service.rebuild(0);

        assertThat(unspent()).containsExactly(hash(2) + "#0").isEqualTo(expected());
    }

    @Test
    void resumesFromAnOutputIndexKeepingTheRowsBeforeIt() {
        for (int n = 0; n < 6; n++)
            output(n, n % 3, 10L * n);
        service.rebuild(0);
        jdbc.update("delete from address_utxo_unspent where output_index = 2");   // an interrupted run stopped in index 2
        jdbc.update("insert into address_utxo_unspent (tx_hash, output_index, slot) values (?, 1, 10)", hash(98));  // must survive: before the resume point

        long copied = service.rebuild(2);

        assertThat(copied).isEqualTo(2);
        assertThat(unspent()).hasSize(7).contains(hash(98) + "#1", hash(2) + "#2", hash(5) + "#2");
    }

    @Test
    void copiesMoreKeysThanOneBatchHolds() {
        int outputs = UnspentUtxoBackfillService.COPY_BATCH_SIZE * 16 * 2 + 100;
        for (int n = 0; n < outputs; n++)
            output(n, 0, n);
        for (int n = 0; n < outputs; n += 10)
            spend(n, 0, n + 1L);

        assertThat(service.rebuild(0)).isEqualTo(outputs - (outputs + 9) / 10);
        assertThat(unspent()).isEqualTo(expected());
    }

    private void cursor(long slot) {
        jdbc.update("insert into cursor_ (id, block_hash, slot, block_number) values (1, ?, ?, ?)", "bh" + slot, slot, slot / 10);
    }

    private Map<String, Object> state() {
        return jdbc.queryForMap("select status, slot, block, block_hash from store_state where state_id = ?", UnspentUtxoBackfillService.STATE_ID);
    }

    private int stateRows() {
        return jdbc.queryForObject("select count(*) from store_state", Integer.class);
    }

    @Test
    void reportsTheCursorAndAnEmptyStore() {
        assertThat(service.cursorSlot()).isNull();
        assertThat(service.maxOutputIndex()).isEqualTo(-1);
        assertThat(service.rebuild(0)).isZero();

        cursor(500);
        cursor(520);
        assertThat(service.cursorSlot()).isEqualTo(520);
    }

    @Test
    void aFinishedRebuildIsRecordedAsCurrentAtTheCursor() {
        output(1, 0, 10);
        cursor(500);
        cursor(520);

        service.rebuild(0);
        assertThat(stateRows()).isZero();   //not current until the caller has checked the cursor did not move
        service.markCurrentAtCursor();

        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("SLOT", 520L)
                .containsEntry("BLOCK", 52L).containsEntry("BLOCK_HASH", "bh520");
    }

    @Test
    void aRebuildDropsTheStateUntilItFinishes() {
        cursor(500);
        service.markCurrentAtCursor();
        assertThat(stateRows()).isOne();

        service.rebuild(3);   //a resumed run that is interrupted again leaves no state

        assertThat(stateRows()).isZero();
    }

    @Test
    void rollbackMovesACurrentStateToThePointAndLeavesOneBehindIt() {
        var named = new NamedParameterJdbcTemplate(jdbc);
        var params = new MapSqlParameterSource("slot", 150L).addValue("block", 15L).addValue("block_hash", "bh150");

        cursor(200);
        service.markCurrentAtCursor();
        named.update(UnspentUtxoBackfillService.STATE_AFTER_ROLLBACK, params);
        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("SLOT", 150L).containsEntry("BLOCK_HASH", "bh150");

        jdbc.update("update store_state set slot = 100");   //kept only to slot 100: behind the point, so not moved
        named.update(UnspentUtxoBackfillService.STATE_AFTER_ROLLBACK, params);
        assertThat(state()).containsEntry("SLOT", 100L);
    }

    @Test
    void rollbackRestoresTheOutputsWhoseSpendIsRolledBack() {
        output(1, 0, 100);                             // spent after the rollback point: restored
        output(2, 0, 100);                             // spent before it: stays out
        output(3, 0, 100);                             // still unspent: already present, not duplicated
        output(4, 0, 200);                             // created after the point: not restored
        spend(1, 0, 300);
        spend(2, 0, 120);
        spend(4, 0, 300);
        service.rebuild(0);
        assertThat(unspent()).containsExactly(hash(3) + "#0");

        var params = new MapSqlParameterSource("slot", 150L);
        int restored = new NamedParameterJdbcTemplate(jdbc).update(UnspentUtxoBackfillService.RESTORE_SPENT_AFTER, params);

        assertThat(restored).isEqualTo(1);
        assertThat(unspent()).containsExactly(hash(1) + "#0", hash(3) + "#0");
    }
}
