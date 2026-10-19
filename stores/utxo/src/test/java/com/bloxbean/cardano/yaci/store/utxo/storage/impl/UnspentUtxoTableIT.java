package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
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

import static org.assertj.core.api.Assertions.assertThat;

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
    void anOutputCreatedAndSpentInTheSameBatchIsGoneAtCommit() {
        utxoStorage.saveSpent(List.of(spend(hash(1), 0, 110)));   // spends first, as UtxoProcessor does
        utxoStorage.saveUnspent(List.of(output(hash(1), 0, 100), output(hash(1), 1, 100)));
        assertThat(unspentKeys()).containsExactly(hash(1) + "#0", hash(1) + "#1");   // written with address_utxo

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
}
