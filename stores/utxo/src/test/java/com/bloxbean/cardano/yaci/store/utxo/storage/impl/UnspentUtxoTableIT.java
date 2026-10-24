package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.service.StoreStateService;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.jooq.DSLContext;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.processor.UtxoRollbackProcessor;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * address_utxo_unspent against a real H2 database with the module's Flyway migrations: written by
 * {@link UtxoStorage} together with address_utxo, it must equal address_utxo minus tx_input after
 * every commit, whatever order the indexer saves, replays or rolls back in.
 */
@SpringBootTest
class UnspentUtxoTableIT {

    private static final String UNIT = "0254a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649";

    @Autowired
    private UtxoRollbackProcessor utxoRollbackProcessor;

    @Autowired
    private UtxoStorage utxoStorage;

    @Autowired
    private UtxoStoreProperties properties;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UtxoRepository utxoRepository;

    @Autowired
    private TxInputRepository txInputRepository;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private StoreStateService storeStateService;

    @BeforeEach
    void enable() {
        properties.setUnspentTableEnabled(true);
    }

    @AfterEach
    void clean() {
        commit();   // drop any spend a test left pending
        jdbc.update("delete from address_utxo_unspent");
        jdbc.update("delete from address_utxo");
        jdbc.update("delete from tx_input");
        jdbc.update("delete from store_state");
        properties.setUnspentTableEnabled(false);
    }

    private static AddressUtxo output(String txHash, int index, long slot) {
        return AddressUtxo.builder()
                .txHash(txHash)
                .outputIndex(index)
                .slot(slot)
                .blockNumber(slot / 10)
                .blockTime(1_600_000L + slot)
                .blockHash("bh" + slot)
                .epoch(1)
                .txIndex(index)
                .ownerAddr("addr_test1" + txHash.substring(0, 8))
                .ownerStakeAddr("stake_test1" + txHash.substring(0, 8))
                .ownerPaymentCredential("pc" + txHash.substring(0, 8))
                .ownerStakeCredential("sc" + txHash.substring(0, 8))
                .lovelaceAmount(BigInteger.valueOf(1_000_000L))
                .amounts(List.of(
                        Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(1_000_000L)).build(),
                        Amt.builder().unit(UNIT).quantity(BigInteger.valueOf(7)).build()))
                .dataHash("dh")
                .inlineDatum("d87980")
                .scriptRef("8200581c")
                .referenceScriptHash("rsh")
                .isCollateralReturn(false)
                .build();
    }

    private static TxInput spend(String txHash, int index, long atSlot) {
        return TxInput.builder()
                .txHash(txHash)
                .outputIndex(index)
                .spentAtSlot(atSlot)
                .spentAtBlock(atSlot / 10)
                .spentAtBlockHash("bh" + atSlot)
                .spentTxHash("spender" + atSlot)
                .build();
    }

    private static String hash(int n) {
        return String.format("%064x", n);
    }

    /** The end of a batch, as both publishers emit it: UtxoProcessor.handleCommit ends with this call. */
    private void commit() {
        utxoStorage.handleCommit(new CommitEvent<>(EventMetadata.builder().build(), List.of()));
    }

    /** The end of a batch whose last block is at {@code slot}. */
    private static CommitEvent<?> commitAt(long slot) {
        return new CommitEvent<>(EventMetadata.builder().slot(slot).block(slot / 10).blockHash("bh" + slot).build(), List.of());
    }

    private Map<String, Object> state() {
        return jdbc.queryForMap("select status, slot, block, block_hash from store_state where state_id = ?", UnspentUtxoTable.STATE_ID);
    }

    private void rollbackTo(long slot) {
        utxoRollbackProcessor.handleRollbackEvent(RollbackEvent.builder().rollbackTo(new Point(slot, "bh" + slot)).build());
    }

    private List<String> unspentKeys() {
        return jdbc.queryForList("select tx_hash || '#' || output_index from address_utxo_unspent order by 1", String.class);
    }

    /** The definition the table must equal: address_utxo minus tx_input. */
    private List<String> expectedKeys() {
        return jdbc.queryForList("""
                select a.tx_hash || '#' || a.output_index from address_utxo a
                where not exists (select 1 from tx_input i where i.tx_hash = a.tx_hash and i.output_index = a.output_index)
                order by 1""", String.class);
    }

    @Test
    void anOutputCreatedAndSpentInTheSameBatchIsNeverWritten() {
        utxoStorage.saveSpent(List.of(spend(hash(1), 0, 110)));   // spends first, as UtxoProcessor does
        utxoStorage.saveUnspent(List.of(output(hash(1), 0, 100), output(hash(1), 1, 100)));
        assertThat(unspentKeys()).isEmpty();   // the batch is settled at commit

        commit();

        assertThat(unspentKeys()).containsExactly(hash(1) + "#1").isEqualTo(expectedKeys());
    }

    @Test
    void anOutputSpentInALaterBatchIsRemovedAtThatBatchesCommit() {
        utxoStorage.saveUnspent(List.of(output(hash(2), 0, 100)));
        commit();
        assertThat(unspentKeys()).containsExactly(hash(2) + "#0");

        utxoStorage.saveSpent(List.of(spend(hash(2), 0, 200)));
        utxoStorage.saveUnspent(List.of(output(hash(3), 0, 200)));
        commit();

        assertThat(unspentKeys()).containsExactly(hash(3) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void theRowKeepsEveryColumnOfTheOutput() {
        utxoStorage.saveUnspent(List.of(output(hash(4), 3, 100)));
        commit();

        String columns = "tx_hash, output_index, slot, block_hash, epoch, lovelace_amount, cast(amounts as varchar) as amounts, "
                + "data_hash, inline_datum, owner_addr, owner_addr_full, owner_stake_addr, owner_payment_credential, "
                + "owner_stake_credential, script_ref, reference_script_hash, is_collateral_return, block, block_time, "
                + "update_datetime, tx_index";
        Map<String, Object> original = jdbc.queryForMap("select " + columns + " from address_utxo");
        Map<String, Object> copy = jdbc.queryForMap("select " + columns + " from address_utxo_unspent");
        assertThat(copy).isEqualTo(original);
    }

    @Test
    void replayingABatchIsHarmless() {
        for (int attempt = 0; attempt < 2; attempt++) {
            utxoStorage.saveSpent(List.of(spend(hash(5), 0, 110)));
            utxoStorage.saveUnspent(List.of(output(hash(5), 0, 100), output(hash(5), 1, 110)));
            commit();
        }

        assertThat(unspentKeys()).containsExactly(hash(5) + "#1").isEqualTo(expectedKeys());
    }

    /**
     * A restart rolls back to the cursor and syncs again from the cursor block, so the first batch holds a block the
     * table has already settled. An output of that block spent in the batch must be deleted, not skipped as new.
     */
    @Test
    void anOutputOfTheCursorBlockSpentAfterARestartIsDeleted() {
        utxoStorage.saveUnspent(List.of(output(hash(50), 0, 200)));
        utxoStorage.handleCommit(commitAt(200));   //the cursor block
        utxoStorage.saveSpent(List.of(spend(hash(50), 0, 230)));
        utxoStorage.saveUnspent(List.of(output(hash(51), 0, 230)));   //killed before this batch's commit

        rollbackTo(200);   //the restart rollback, to the cursor
        utxoStorage.saveSpent(List.of(spend(hash(50), 0, 230)));
        utxoStorage.saveUnspent(List.of(output(hash(50), 0, 200), output(hash(51), 0, 230)));
        utxoStorage.handleCommit(commitAt(299));

        assertThat(unspentKeys()).containsExactly(hash(51) + "#0").isEqualTo(expectedKeys());
    }

    /**
     * A crash after the batch's commit but before the cursor moved: the restart rollback to the cursor restores the
     * output the batch spent, and the replayed batch, which creates and spends it, must delete it again.
     */
    @Test
    void anOutputRestoredByTheRestartRollbackIsDeletedByTheReplayedBatch() {
        utxoStorage.saveUnspent(List.of(output(hash(52), 0, 200)));
        utxoStorage.handleCommit(commitAt(200));   //the cursor block
        utxoStorage.saveSpent(List.of(spend(hash(52), 0, 230)));
        utxoStorage.saveUnspent(List.of(output(hash(53), 0, 230)));
        utxoStorage.handleCommit(commitAt(299));   //committed, then killed before the cursor moved
        assertThat(unspentKeys()).containsExactly(hash(53) + "#0");

        rollbackTo(200);
        assertThat(unspentKeys()).containsExactly(hash(52) + "#0");   //restored: its spend is after the point
        utxoStorage.saveSpent(List.of(spend(hash(52), 0, 230)));
        utxoStorage.saveUnspent(List.of(output(hash(52), 0, 200), output(hash(53), 0, 230)));
        utxoStorage.handleCommit(commitAt(299));

        assertThat(unspentKeys()).containsExactly(hash(53) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void anOlderOutputReSavedInTheBatchThatSpendsItIsDeleted() {
        utxoStorage.saveUnspent(List.of(output(hash(54), 0, 100)));
        utxoStorage.handleCommit(commitAt(100));

        utxoStorage.saveSpent(List.of(spend(hash(54), 0, 230)));
        utxoStorage.saveUnspent(List.of(output(hash(54), 0, 100)));   //pointer-address re-save at commit
        utxoStorage.handleCommit(commitAt(299));

        assertThat(unspentKeys()).isEmpty();
        assertThat(expectedKeys()).isEmpty();
    }

    @Test
    void withoutACurrentStateAnOutputSpentInTheBatchIsDeleted() {
        utxoStorage.saveUnspent(List.of(output(hash(55), 0, 200)));
        utxoStorage.handleCommit(commitAt(200));
        jdbc.update("delete from store_state");

        utxoStorage.saveSpent(List.of(spend(hash(55), 0, 230)));
        utxoStorage.saveUnspent(List.of(output(hash(55), 0, 200)));
        utxoStorage.handleCommit(commitAt(299));

        assertThat(unspentKeys()).isEmpty();
        assertThat(expectedKeys()).isEmpty();
    }

    @Test
    void rollbackRemovesNewerOutputsAndRestoresOnesSpentAfterThePoint() {
        utxoStorage.saveUnspent(List.of(output(hash(6), 0, 100)));
        commit();
        utxoStorage.saveSpent(List.of(spend(hash(6), 0, 200)));
        utxoStorage.saveUnspent(List.of(output(hash(7), 0, 200)));
        commit();
        assertThat(unspentKeys()).containsExactly(hash(7) + "#0");

        rollbackTo(150);

        assertThat(unspentKeys()).containsExactly(hash(6) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void spendsOfARolledBackBatchAreNotAppliedAtTheNextCommit() {
        utxoStorage.saveUnspent(List.of(output(hash(8), 0, 100)));
        commit();
        utxoStorage.saveSpent(List.of(spend(hash(8), 0, 200)));   // batch interrupted before its commit

        rollbackTo(150);
        commit();

        assertThat(unspentKeys()).containsExactly(hash(8) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void reSavingAnOutputAtCommitUpdatesItsRow() {
        utxoStorage.saveUnspent(List.of(output(hash(9), 0, 100)));
        commit();

        AddressUtxo resolved = output(hash(9), 0, 100);   // pointer address resolved at commit
        resolved.setOwnerStakeAddr("stake_test1resolved");
        utxoStorage.saveUnspent(List.of(resolved));
        commit();

        assertThat(jdbc.queryForObject("select owner_stake_addr from address_utxo_unspent", String.class))
                .isEqualTo("stake_test1resolved");
    }

    @Test
    void reSavingAnOutputSpentInTheSameBatchDoesNotBringItBack() {
        utxoStorage.saveUnspent(List.of(output(hash(10), 0, 100)));
        utxoStorage.saveSpent(List.of(spend(hash(10), 0, 110)));
        utxoStorage.saveUnspent(List.of(output(hash(10), 0, 100)));   // pointer-address re-save runs before the delete

        commit();

        assertThat(unspentKeys()).isEmpty();
        assertThat(expectedKeys()).isEmpty();
    }

    @Test
    void outputsOfARolledBackBatchAreNotWrittenAtTheNextCommit() {
        utxoStorage.saveUnspent(List.of(output(hash(41), 0, 100)));
        commit();
        utxoStorage.saveUnspent(List.of(output(hash(42), 0, 200)));   // batch interrupted before its commit

        rollbackTo(150);
        commit();

        assertThat(unspentKeys()).containsExactly(hash(41) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void aLargeBatchWithOutputsSpentInItIsSettledInOneCommit() {
        List<AddressUtxo> outputs = new ArrayList<>();
        List<TxInput> spends = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            outputs.add(output(hash(20_000 + i), 0, 100));
            if (i % 5 != 0)
                spends.add(spend(hash(20_000 + i), 0, 110));
        }
        utxoStorage.saveUnspent(List.of(output(hash(19_999), 0, 50)));
        commit();
        spends.add(spend(hash(19_999), 0, 110));   // an output of the previous batch

        utxoStorage.saveSpent(spends);
        utxoStorage.saveUnspent(outputs);
        commit();

        assertThat(unspentKeys()).hasSize(500).isEqualTo(expectedKeys());
    }

    @Test
    void genesisOutputsAreKept() {
        utxoStorage.saveUnspent(List.of(output(hash(11), 0, -1)));
        commit();

        assertThat(unspentKeys()).containsExactly(hash(11) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void aLargeBatchOfSpendsIsDeletedInOneCommit() {
        List<AddressUtxo> outputs = new ArrayList<>();
        List<TxInput> spends = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            outputs.add(output(hash(10_000 + i), 0, 100));
            if (i % 5 != 0)
                spends.add(spend(hash(10_000 + i), 0, 200));
        }
        utxoStorage.saveUnspent(outputs);
        commit();
        utxoStorage.saveSpent(spends);
        commit();

        assertThat(unspentKeys()).hasSize(500).isEqualTo(expectedKeys());
    }

    @Test
    void nothingIsWrittenWhileTheTableIsDisabled() {
        properties.setUnspentTableEnabled(false);
        utxoStorage.saveUnspent(List.of(output(hash(12), 0, 100)));
        utxoStorage.saveSpent(List.of(spend(hash(12), 0, 110)));
        commit();
        rollbackTo(50);

        assertThat(unspentKeys()).isEmpty();
    }

    /** A store with its own table and cursor, so the shared beans stay in from-genesis mode with no cursor. */
    private record Store(UnspentUtxoTable table, UtxoStorageImpl storage, TransactionTemplate tx) {
        void commit() {
            storage.handleCommit(new CommitEvent<>(EventMetadata.builder().build(), List.of()));
        }

        void commit(long slot) {
            storage.handleCommit(commitAt(slot));
        }

        void reachTip() {
            table.handlePreBlockSync(new PreBlockSyncEvent(new Point(0, "tip")));
        }

        /** As UtxoRollbackProcessor does, in a transaction: this storage is not a Spring proxy. */
        void rollbackTo(long slot) {
            tx.executeWithoutResult(status -> {
                storage.deleteUnspentBySlotGreaterThan(slot);
                storage.deleteSpentBySlotGreaterThan(slot);
            });
        }
    }

    private Store store(UtxoStoreProperties storeProperties, Long cursorSlot) {
        CursorService cursorService = mock(CursorService.class);
        when(cursorService.getCursor()).thenReturn(cursorSlot == null ? Optional.empty()
                : Optional.of(Cursor.builder().slot(cursorSlot).block(cursorSlot / 10).blockHash("bh" + cursorSlot).build()));
        UnspentUtxoTable table = new UnspentUtxoTable(storeProperties, dsl, transactionManager, storeStateService, cursorService);
        return new Store(table, new UtxoStorageImpl(utxoRepository, txInputRepository, dsl, new UtxoCache(), transactionManager, table),
                new TransactionTemplate(transactionManager));
    }

    private Store atTip() {
        return store(UtxoStoreProperties.builder()
                .unspentTableEnabled(true).unspentTableBackfillAtTip(true).unspentTableReadEnabled(true).build(), null);
    }

    /** A store kept from genesis whose cursor is at {@code cursorSlot}. */
    private Store fromGenesisAt(Long cursorSlot) {
        return store(UtxoStoreProperties.builder().unspentTableEnabled(true).unspentTableReadEnabled(true).build(), cursorSlot);
    }

    /** The same store running with the table turned off. */
    private Store tableOff() {
        return store(UtxoStoreProperties.builder().build(), null);
    }

    @Test
    void backfillAtTipWritesNothingDuringTheSyncThenBuildsAtTheTipAndKeepsTheTableCurrent() {
        Store store = atTip();
        store.storage().saveUnspent(List.of(output(hash(20), 0, -1), output(hash(20), 1, 100), output(hash(21), 0, 100), output(hash(21), 2, 100)));
        store.storage().saveSpent(List.of(spend(hash(20), 1, 110)));
        store.commit();
        assertThat(unspentKeys()).isEmpty();
        assertThat(store.table().isActive()).isFalse();
        assertThat(store.table().isReadable()).isFalse();

        store.reachTip();

        assertThat(store.table().isActive()).isTrue();
        assertThat(store.table().isReadable()).isTrue();
        assertThat(unspentKeys()).hasSize(3).isEqualTo(expectedKeys());

        store.storage().saveSpent(List.of(spend(hash(21), 2, 200)));   //the next block, kept by UtxoStorageImpl
        store.storage().saveUnspent(List.of(output(hash(22), 0, 200)));
        store.commit();
        assertThat(unspentKeys()).hasSize(3).isEqualTo(expectedKeys());

        store.reachTip();   //built already: a later sync start (restart, reconnect) changes nothing
        assertThat(unspentKeys()).hasSize(3).isEqualTo(expectedKeys());
    }

    @Test
    void aTableRecordedAsCurrentIsUsedAsIs() {
        utxoStorage.saveUnspent(List.of(output(hash(23), 0, 100)));   //written by the shared store, from genesis
        commit();

        Store store = atTip();
        assertThat(store.table().isActive()).isTrue();
        store.storage().saveSpent(List.of(spend(hash(23), 0, 200)));
        store.commit();

        assertThat(unspentKeys()).isEmpty();
    }

    @Test
    void backfillAtTipCopiesMoreKeysThanOneBatchAcrossEveryTxHashSlice() {
        List<AddressUtxo> outputs = new ArrayList<>();
        List<TxInput> spends = new ArrayList<>();
        int count = UnspentUtxoTable.COPY_BATCH_SIZE * 2 + 300;
        for (int i = 0; i < count; i++) {
            String txHash = Integer.toHexString(i % 16) + String.format("%063x", i);   //every first hex digit
            outputs.add(output(txHash, i % 3, 100));
            if (i % 7 == 0)
                spends.add(spend(txHash, i % 3, 150));
        }
        Store store = atTip();
        store.storage().saveUnspent(outputs);
        store.storage().saveSpent(spends);
        store.commit();

        store.reachTip();

        assertThat(unspentKeys()).hasSize(count - spends.size()).isEqualTo(expectedKeys());
    }

    @Test
    void eachCommitRecordsTheBatchsLastBlock() {
        utxoStorage.saveUnspent(List.of(output(hash(30), 0, 100)));
        utxoStorage.handleCommit(commitAt(120));

        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("SLOT", 120L)
                .containsEntry("BLOCK", 12L).containsEntry("BLOCK_HASH", "bh120");
    }

    @Test
    void aTableBehindTheCursorIsNotUsedAndIsBuiltAgainAtTheTip() {
        utxoStorage.saveUnspent(List.of(output(hash(31), 0, 100), output(hash(32), 0, 100)));
        utxoStorage.handleCommit(commitAt(100));

        Store off = tableOff();   //the store runs on to slot 200 with the table off
        off.storage().saveSpent(List.of(spend(hash(31), 0, 200)));
        off.storage().saveUnspent(List.of(output(hash(33), 0, 200)));
        off.commit(200);
        assertThat(unspentKeys()).containsExactly(hash(31) + "#0", hash(32) + "#0");   //kept to 100 only

        Store on = fromGenesisAt(200L);   //turned back on
        assertThat(on.table().isActive()).isFalse();
        assertThat(on.table().isReadable()).isFalse();
        on.storage().saveUnspent(List.of(output(hash(34), 0, 210)));   //not written while not current
        on.commit(210);
        assertThat(unspentKeys()).containsExactly(hash(31) + "#0", hash(32) + "#0");

        on.reachTip();

        assertThat(on.table().isActive()).isTrue();
        assertThat(on.table().isReadable()).isTrue();
        assertThat(unspentKeys()).hasSize(3).isEqualTo(expectedKeys());
        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("SLOT", 200L);
    }

    @Test
    void aCommitAheadOfTheCursorKeepsTheTable() {
        utxoStorage.saveUnspent(List.of(output(hash(35), 0, 100)));
        utxoStorage.handleCommit(commitAt(200));   //a crash before the cursor moved: the batch is replayed

        assertThat(fromGenesisAt(100L).table().isActive()).isTrue();
    }

    /** The end of a batch of blocks from {@code firstSlot} to {@code lastSlot}, one every 10 slots (block = slot / 10). */
    private static CommitEvent<?> batch(long firstSlot, long lastSlot) {
        List<BatchBlock> blocks = new ArrayList<>();
        for (long slot = firstSlot; slot <= lastSlot; slot += 10)
            blocks.add(new BatchBlock(EventMetadata.builder().slot(slot).block(slot / 10).blockHash("bh" + slot).build(), null, List.of()));
        return new CommitEvent<>(blocks.getLast().getMetadata(), blocks);
    }

    @Test
    void aBatchThatFollowsOnOrRepeatsABlockKeepsTheTable() {
        Store store = fromGenesisAt(null);
        store.storage().saveUnspent(List.of(output(hash(56), 0, 100)));
        store.storage().handleCommit(batch(10, 100));      //blocks 1 to 10
        store.storage().saveUnspent(List.of(output(hash(57), 0, 200)));
        store.storage().handleCommit(batch(110, 200));     //blocks 11 to 20: right after
        store.storage().saveUnspent(List.of(output(hash(58), 0, 300)));
        store.storage().handleCommit(batch(200, 300));     //blocks 20 to 30: block 20 again, as after a restart

        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("BLOCK", 30L);
        assertThat(store.table().isActive()).isTrue();
        assertThat(unspentKeys()).hasSize(3).isEqualTo(expectedKeys());
    }

    @Test
    void aBatchAfterSkippedBlocksSwitchesTheTableOffUntilItIsBuiltAgain() {
        Store store = fromGenesisAt(null);
        store.storage().saveUnspent(List.of(output(hash(59), 0, 100)));
        store.storage().handleCommit(batch(10, 100));      //the table holds blocks up to 10

        store.storage().saveSpent(List.of(spend(hash(59), 0, 300)));
        store.storage().saveUnspent(List.of(output(hash(60), 0, 300)));
        store.storage().handleCommit(batch(300, 310));     //starts at block 30: blocks 11 to 29 never reached the table

        assertThat(state()).containsEntry("STATUS", "STALE").containsEntry("BLOCK", 10L);
        assertThat(store.table().isActive()).isFalse();
        assertThat(store.table().isReadable()).isFalse();   //queries use address_utxo and tx_input
        assertThat(unspentKeys()).containsExactly(hash(59) + "#0");   //left as it was, not written further

        store.storage().saveUnspent(List.of(output(hash(61), 0, 320)));   //the sync keeps running
        store.storage().handleCommit(batch(320, 320));
        assertThat(unspentKeys()).containsExactly(hash(59) + "#0");

        store.reachTip();   //the next sync start builds it again

        assertThat(store.table().isActive()).isTrue();
        assertThat(state()).containsEntry("STATUS", "CURRENT");
        assertThat(unspentKeys()).hasSize(2).isEqualTo(expectedKeys());
    }

    @Test
    void aRollbackWithTheTableOnMovesItsStateToThePoint() {
        utxoStorage.saveUnspent(List.of(output(hash(36), 0, 100)));
        utxoStorage.handleCommit(commitAt(200));

        rollbackTo(150);

        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("SLOT", 150L);
        assertThat(fromGenesisAt(150L).table().isActive()).isTrue();
    }

    @Test
    void aRollbackWhileTheTableIsOffMarksItStale() {
        utxoStorage.saveUnspent(List.of(output(hash(37), 0, 100), output(hash(38), 0, 180)));
        utxoStorage.handleCommit(commitAt(200));

        tableOff().rollbackTo(150);   //hash(38) is gone from address_utxo but not from the table

        assertThat(state()).containsEntry("STATUS", "STALE").containsEntry("SLOT", 200L);
        assertThat(fromGenesisAt(150L).table().isActive()).isFalse();
    }

    @Test
    void aRollbackWhileTheTableIsOffLeavesAStateBeforeThePointAlone() {
        utxoStorage.saveUnspent(List.of(output(hash(39), 0, 100)));
        utxoStorage.handleCommit(commitAt(100));

        tableOff().rollbackTo(150);

        assertThat(state()).containsEntry("STATUS", "CURRENT").containsEntry("SLOT", 100L);
    }

    @Test
    void rowsWithoutAStateAreNotTrusted() {
        jdbc.update("insert into address_utxo_unspent (tx_hash, output_index, slot) values (?, 0, 100)", hash(40));

        assertThat(fromGenesisAt(null).table().isActive()).isFalse();
        assertThat(fromGenesisAt(500L).table().isActive()).isFalse();
    }
}
