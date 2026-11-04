package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UnspentUtxoTableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The address_utxo_unspent table against a real H2 database with the module's Flyway
 * migrations: it must always equal address_utxo minus tx_input, whatever order the indexer
 * commits, replays or rolls back in.
 */
@SpringBootTest
class UnspentUtxoTableProcessorIT {

    private static final String UNIT = "0254a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649";

    @Autowired
    private UnspentUtxoTableProcessor processor;

    @Autowired
    private UtxoRollbackProcessor utxoRollbackProcessor;

    @Autowired
    private UnspentUtxoTableService service;

    @Autowired
    private UtxoStorage utxoStorage;

    @Autowired
    private UtxoStoreProperties properties;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void enable() {
        properties.setUnspentTableEnabled(true);
    }

    @AfterEach
    void clean() {
        jdbc.update("delete from address_utxo_unspent");
        jdbc.update("delete from address_utxo");
        jdbc.update("delete from tx_input");
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

    /** A commit of the blocks at these slots, as the parallel publisher emits it. */
    private static CommitEvent<BatchBlock> commit(long... slots) {
        List<BatchBlock> blocks = LongStream.of(slots)
                .mapToObj(s -> new BatchBlock(EventMetadata.builder().slot(s).build(), null, null))
                .toList();
        return new CommitEvent<>(EventMetadata.builder().slot(slots[slots.length - 1]).build(), blocks);
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
    void anOutputCreatedAndSpentInTheSameBatchNeverAppears() {
        utxoStorage.saveSpent(List.of(spend(hash(1), 0, 110)));   // spends first, as UtxoProcessor does
        utxoStorage.saveUnspent(List.of(output(hash(1), 0, 100), output(hash(1), 1, 100)));

        processor.handleCommit(commit(100, 110));

        assertThat(unspentKeys()).containsExactly(hash(1) + "#1").isEqualTo(expectedKeys());
    }

    @Test
    void anOutputSpentInALaterBatchIsRemovedAtThatBatchesCommit() {
        utxoStorage.saveUnspent(List.of(output(hash(2), 0, 100)));
        processor.handleCommit(commit(100));
        assertThat(unspentKeys()).containsExactly(hash(2) + "#0");

        utxoStorage.saveSpent(List.of(spend(hash(2), 0, 200)));
        utxoStorage.saveUnspent(List.of(output(hash(3), 0, 200)));
        processor.handleCommit(commit(200));

        assertThat(unspentKeys()).containsExactly(hash(3) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void theCopyKeepsEveryColumnOfTheOutput() {
        utxoStorage.saveUnspent(List.of(output(hash(4), 3, 100)));
        processor.handleCommit(commit(100));

        String columns = "tx_hash, output_index, slot, block_hash, epoch, lovelace_amount, cast(amounts as varchar) as amounts, "
                + "data_hash, owner_addr, owner_stake_addr, owner_payment_credential, owner_stake_credential, "
                + "is_collateral_return, block, block_time, tx_index";
        Map<String, Object> original = jdbc.queryForMap("select " + columns + " from address_utxo");
        Map<String, Object> copy = jdbc.queryForMap("select " + columns + " from address_utxo_unspent");
        assertThat(copy).isEqualTo(original);
    }

    @Test
    void replayingACommittedBatchIsHarmless() {
        utxoStorage.saveSpent(List.of(spend(hash(5), 0, 110)));
        utxoStorage.saveUnspent(List.of(output(hash(5), 0, 100), output(hash(5), 1, 110)));

        processor.handleCommit(commit(100, 110));
        processor.handleCommit(commit(100, 110));

        assertThat(unspentKeys()).containsExactly(hash(5) + "#1").isEqualTo(expectedKeys());
    }

    @Test
    void rollbackRemovesNewerOutputsAndRestoresOnesSpentAfterThePoint() {
        utxoStorage.saveUnspent(List.of(output(hash(6), 0, 100)));
        processor.handleCommit(commit(100));
        utxoStorage.saveSpent(List.of(spend(hash(6), 0, 200)));
        utxoStorage.saveUnspent(List.of(output(hash(7), 0, 200)));
        processor.handleCommit(commit(200));
        assertThat(unspentKeys()).containsExactly(hash(7) + "#0");

        RollbackEvent rollback = RollbackEvent.builder().rollbackTo(new Point(150, "bh150")).build();
        processor.handleRollback(rollback);          // runs before UtxoRollbackProcessor (ordered)
        utxoRollbackProcessor.handleRollbackEvent(rollback);

        assertThat(unspentKeys()).containsExactly(hash(6) + "#0").isEqualTo(expectedKeys());
    }

    @Test
    void nothingIsWrittenWhileTheTableIsDisabled() {
        properties.setUnspentTableEnabled(false);
        utxoStorage.saveUnspent(List.of(output(hash(8), 0, 100)));

        processor.handleCommit(commit(100));
        processor.handleRollback(RollbackEvent.builder().rollbackTo(new Point(50, "bh50")).build());

        assertThat(unspentKeys()).isEmpty();
    }

    @Test
    void backfillCopiesTheUnspentOutputsOfARangeInChunksAndCanBeRepeated() {
        for (int i = 0; i < 25; i++) {
            utxoStorage.saveUnspent(List.of(output(hash(100 + i), 0, 10L * i)));
        }
        utxoStorage.saveSpent(List.of(spend(hash(103), 0, 500), spend(hash(117), 0, 500)));

        long copied = service.backfill(0, 240, 7);
        long again = service.backfill(0, 240, 7);

        assertThat(copied).isEqualTo(23);
        assertThat(again).isZero();
        assertThat(unspentKeys()).hasSize(23).isEqualTo(expectedKeys());
    }

    @Test
    void reconcileRemovesRowsABackfillCopiedBeforeTheirSpendWasWritten() {
        utxoStorage.saveUnspent(List.of(output(hash(9), 0, 100), output(hash(10), 0, 100)));
        service.backfill(0, 100, 50);                       // a backfill read both as unspent
        utxoStorage.saveSpent(List.of(spend(hash(9), 0, 300)));   // the indexer then spent one

        long removed = service.reconcileSpentSince(250);

        assertThat(removed).isEqualTo(1);
        assertThat(unspentKeys()).containsExactly(hash(10) + "#0").isEqualTo(expectedKeys());
    }
}
