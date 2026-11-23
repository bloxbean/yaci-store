package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "store.utxo.pruning-batch-size=3")
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
            insertSpentOutput(txHash(i), 100L);
        // spent after the pruning block: must stay
        insertSpentOutput(txHash(10), 200L);
        // unspent: must stay
        insertOutput(txHash(20));

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

    private void insertSpentOutput(String txHash, long spentAtBlock) {
        insertOutput(txHash);
        dsl.insertInto(TX_INPUT)
                .set(TX_INPUT.TX_HASH, txHash)
                .set(TX_INPUT.OUTPUT_INDEX, 0)
                .set(TX_INPUT.SPENT_AT_BLOCK, spentAtBlock)
                .set(TX_INPUT.SPENT_AT_SLOT, spentAtBlock * 10)
                .execute();
    }

    private void insertOutput(String txHash) {
        dsl.insertInto(ADDRESS_UTXO)
                .set(ADDRESS_UTXO.TX_HASH, txHash)
                .set(ADDRESS_UTXO.OUTPUT_INDEX, 0)
                .set(ADDRESS_UTXO.BLOCK, 50L)
                .set(ADDRESS_UTXO.SLOT, 500L)
                .execute();
    }

    private static String txHash(int i) {
        return String.format("%064x", i);
    }
}
