package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An API-only instance ({@code store.read-only-mode=true}) has no CursorService: it never syncs, another instance
 * writes the table and its store_state row. It must read the table only while that row is CURRENT and not behind the
 * cursor in the database, and notice when the writer changes it.
 */
@SpringBootTest(properties = "store.read-only-mode=true")
class UnspentUtxoTableReadOnlyIT {

    @Autowired
    private UnspentUtxoTable table;

    @Autowired
    private UtxoStoreProperties properties;

    @Autowired
    private StoreProperties storeProperties;

    @Autowired
    private ObjectProvider<CursorService> cursorService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void enable() {
        properties.setUnspentTableEnabled(true);
        properties.setUnspentTableReadEnabled(true);
        table.readOnlyRecheckMillis = 0;   //look at the database on every call
    }

    @AfterEach
    void clean() {
        jdbc.update("delete from store_state");
        jdbc.update("delete from cursor_");
        properties.setUnspentTableEnabled(false);
        properties.setUnspentTableReadEnabled(false);
    }

    private void cursorAt(long slot) {
        jdbc.update("insert into cursor_ (id, block_hash, slot, block_number) values (?, ?, ?, ?)",
                storeProperties.getEventPublisherId(), "bh" + slot, slot, slot / 10);
    }

    private void state(String status, long slot) {
        jdbc.update("delete from store_state where state_id = ?", UnspentUtxoTable.STATE_ID);
        jdbc.update("insert into store_state (state_id, status, slot, block, block_hash) values (?, ?, ?, ?, ?)",
                UnspentUtxoTable.STATE_ID, status, slot, slot / 10, "bh" + slot);
    }

    @Test
    void theContextIsReadOnly() {
        assertThat(cursorService.getIfAvailable()).isNull();
    }

    @Test
    void aTableBehindTheCursorIsNotRead() {
        //The writer ran with the table off from slot 100 to 200: the row stayed CURRENT at 100
        state(UnspentUtxoTable.STATUS_CURRENT, 100);
        cursorAt(200);

        assertThat(table.isReadable()).isFalse();
    }

    @Test
    void aTableAtOrAheadOfTheCursorIsRead() {
        state(UnspentUtxoTable.STATUS_CURRENT, 200);
        cursorAt(200);
        assertThat(table.isReadable()).isTrue();

        state(UnspentUtxoTable.STATUS_CURRENT, 300);   //a commit ahead of the cursor
        assertThat(table.isReadable()).isTrue();
    }

    @Test
    void aTableTheWriterMarksStaleIsNoLongerRead() {
        state(UnspentUtxoTable.STATUS_CURRENT, 200);
        cursorAt(200);
        assertThat(table.isReadable()).isTrue();

        state(UnspentUtxoTable.STATUS_STALE, 200);   //the writer found a gap
        assertThat(table.isReadable()).isFalse();

        state(UnspentUtxoTable.STATUS_CURRENT, 250);   //rebuilt and switched on again
        cursorAt(250);
        assertThat(table.isReadable()).isTrue();
    }

    @Test
    void aTableThatFallsBehindTheCursorIsNoLongerRead() {
        state(UnspentUtxoTable.STATUS_CURRENT, 200);
        cursorAt(200);
        assertThat(table.isReadable()).isTrue();

        cursorAt(260);   //the writer moved on without the table

        assertThat(table.isReadable()).isFalse();
    }

    @Test
    void withoutAStateTheTableIsNotRead() {
        cursorAt(200);
        assertThat(table.isReadable()).isFalse();

        jdbc.update("delete from cursor_");
        assertThat(table.isReadable()).isFalse();
    }

    @Test
    void theReadFlagStillDecides() {
        state(UnspentUtxoTable.STATUS_CURRENT, 200);
        cursorAt(200);
        properties.setUnspentTableReadEnabled(false);

        assertThat(table.isReadable()).isFalse();
    }
}
