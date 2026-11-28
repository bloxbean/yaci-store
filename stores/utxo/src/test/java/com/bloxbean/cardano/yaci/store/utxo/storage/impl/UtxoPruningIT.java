package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "store.utxo.pruning-batch-size=3",
        "spring.datasource.url=jdbc:h2:mem:utxo_pruning"
})
class UtxoPruningIT {

    @Autowired
    private UtxoStorage utxoStorage;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void cleanUp() {
        dsl.deleteFrom(TX_INPUT).execute();
        dsl.deleteFrom(ADDRESS_UTXO).execute();
    }

    @Test
    void deleteBySpentAndBlockLessThan_batchLimitInsideSameBlock_neverLeavesSpentOutputWithoutTxInput() {
        // 5 outputs spent in the same block 100: batch size 3 cuts through this group
        for (int i = 0; i < 5; i++)
            insertSpentOutput(txHash(i), 0, 100L);
        // spent after the pruning block: must stay
        insertSpentOutput(txHash(10), 0, 200L);
        // unspent: must stay
        insertOutput(txHash(20), 0);

        int deleted = utxoStorage.deleteBySpentAndBlockLessThan(150L);

        assertThat(deleted).isEqualTo(10); // 5 address_utxo + 5 tx_input
        assertThat(dsl.fetchCount(TX_INPUT)).isEqualTo(1);
        assertThat(dsl.fetchCount(ADDRESS_UTXO)).isEqualTo(2);

        // the only address_utxo row without a tx_input must be the real unspent one
        var unspent = dsl.select(ADDRESS_UTXO.TX_HASH)
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT).on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH)
                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))
                .where(TX_INPUT.TX_HASH.isNull())
                .fetch(ADDRESS_UTXO.TX_HASH);
        assertThat(unspent).containsExactly(txHash(20));
    }

    @Test
    void deleteBySpentAndBlockLessThan_sameTxHash_deletesOnlyMatchingOutputIndex() {
        String tx = txHash(1);
        insertSpentOutput(tx, 0, 100L); // pruned
        insertOutput(tx, 1);            // unspent: must stay
        insertSpentOutput(tx, 2, 200L); // spent after the pruning block: must stay

        int deleted = utxoStorage.deleteBySpentAndBlockLessThan(150L);

        assertThat(deleted).isEqualTo(2);
        assertThat(dsl.select(ADDRESS_UTXO.OUTPUT_INDEX).from(ADDRESS_UTXO).where(ADDRESS_UTXO.TX_HASH.eq(tx))
                .fetch(ADDRESS_UTXO.OUTPUT_INDEX))
                .containsExactlyInAnyOrder(1, 2);
        assertThat(dsl.select(TX_INPUT.OUTPUT_INDEX).from(TX_INPUT).where(TX_INPUT.TX_HASH.eq(tx))
                .fetch(TX_INPUT.OUTPUT_INDEX))
                .containsExactly(2);
    }

    @Test
    void deleteBySpentAndBlockLessThan_batchLargerThanDeleteChunk_deletesAllChunks() {
        // batch 2500 -> delete chunks of 1000 + 1000 + 500, then a second batch of 100
        Object target = AopTestUtils.getTargetObject(utxoStorage);
        ReflectionTestUtils.setField(target, "pruningBatchSize", 2500);
        try {
            for (int i = 0; i < 2600; i++)
                insertSpentOutput(txHash(i), i % 3, 100L);
            insertOutput(txHash(5000), 0);

            int deleted = utxoStorage.deleteBySpentAndBlockLessThan(150L);

            assertThat(deleted).isEqualTo(5200);
            assertThat(dsl.fetchCount(TX_INPUT)).isZero();
            assertThat(dsl.select(ADDRESS_UTXO.TX_HASH).from(ADDRESS_UTXO).fetch(ADDRESS_UTXO.TX_HASH))
                    .containsExactly(txHash(5000));
        } finally {
            ReflectionTestUtils.setField(target, "pruningBatchSize", 3);
        }
    }

    @Test
    void deleteBySpentAndBlockLessThan_spentAtPruningBlock_isKept() {
        insertSpentOutput(txHash(1), 0, 150L);

        int deleted = utxoStorage.deleteBySpentAndBlockLessThan(150L);

        assertThat(deleted).isZero();
        assertThat(dsl.fetchCount(TX_INPUT)).isEqualTo(1);
        assertThat(dsl.fetchCount(ADDRESS_UTXO)).isEqualTo(1);
    }

    @Test
    void deleteBySpentAndBlockLessThan_nothingToPrune_returnsZero() {
        insertOutput(txHash(1), 0);

        assertThat(utxoStorage.deleteBySpentAndBlockLessThan(150L)).isZero();
        assertThat(dsl.fetchCount(ADDRESS_UTXO)).isEqualTo(1);
    }

    @Test
    void deleteBySpentAndBlockLessThan_txInputWithoutAddressUtxo_isDeleted() {
        insertTxInput(txHash(1), 0, 100L);

        int deleted = utxoStorage.deleteBySpentAndBlockLessThan(150L);

        assertThat(deleted).isEqualTo(1);
        assertThat(dsl.fetchCount(TX_INPUT)).isZero();
    }

    private void insertSpentOutput(String txHash, int outputIndex, long spentAtBlock) {
        insertOutput(txHash, outputIndex);
        insertTxInput(txHash, outputIndex, spentAtBlock);
    }

    private void insertTxInput(String txHash, int outputIndex, long spentAtBlock) {
        dsl.insertInto(TX_INPUT)
                .set(TX_INPUT.TX_HASH, txHash)
                .set(TX_INPUT.OUTPUT_INDEX, outputIndex)
                .set(TX_INPUT.SPENT_AT_BLOCK, spentAtBlock)
                .set(TX_INPUT.SPENT_AT_SLOT, spentAtBlock * 10)
                .execute();
    }

    private void insertOutput(String txHash, int outputIndex) {
        dsl.insertInto(ADDRESS_UTXO)
                .set(ADDRESS_UTXO.TX_HASH, txHash)
                .set(ADDRESS_UTXO.OUTPUT_INDEX, outputIndex)
                .set(ADDRESS_UTXO.BLOCK, 50L)
                .set(ADDRESS_UTXO.SLOT, 500L)
                .execute();
    }

    private static String txHash(int i) {
        return String.format("%064x", i);
    }
}
