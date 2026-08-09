package com.bloxbean.cardano.yaci.store.blockfrost.network.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.network.storage.BFNetworkStorageReader;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.WITHDRAWAL;
import static org.jooq.impl.DSL.*;

@Component
@RequiredArgsConstructor
public class BFNetworkStorageReaderImpl implements BFNetworkStorageReader {

    private final DSLContext dsl;

    @Override
    public BigInteger getLockedSupply() {
        return BigInteger.ZERO;
    }

    /**
     * Circulating supply formula (mirrors Blockfrost / cardano-db-sync):
     * SUM(unspent UTxO lovelace) + SUM(spendable reward) + SUM(spendable reward_rest) − SUM(withdrawals)
     */
    @Override
    public BigInteger getCirculatingSupply(int currentEpoch) {
        BigDecimal utxo = dsl.select(sum(ADDRESS_UTXO.LOVELACE_AMOUNT))
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                    .on(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH)
                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX)))
                .where(TX_INPUT.TX_HASH.isNull())
                .fetchOneInto(BigDecimal.class);

        BigDecimal rewards = dsl.select(sum(REWARD.AMOUNT))
                .from(REWARD)
                .where(REWARD.SPENDABLE_EPOCH.le(currentEpoch))
                .fetchOneInto(BigDecimal.class);

        BigDecimal rewardRest = dsl.select(sum(REWARD_REST.AMOUNT))
                .from(REWARD_REST)
                .where(REWARD_REST.SPENDABLE_EPOCH.le(currentEpoch))
                .fetchOneInto(BigDecimal.class);

        BigDecimal withdrawals = dsl.select(sum(WITHDRAWAL.AMOUNT))
                .from(WITHDRAWAL)
                .fetchOneInto(BigDecimal.class);

        BigDecimal result = coalesce(utxo)
                .add(coalesce(rewards))
                .add(coalesce(rewardRest))
                .subtract(coalesce(withdrawals));

        return result.toBigInteger();
    }

    @Override
    public BigInteger getLiveStake() {
        // live stake requires the node's in-memory mark snapshot (Local State Query).
        // Cannot be reproduced from on-chain DB tables — see docs/NETWORK_API_INVESTIGATION.md §3.
        return BigInteger.ZERO;
    }

    @Override
    public int getCurrentEpoch() {
        Integer epoch = dsl.select(max(ADAPOT.EPOCH))
                .from(ADAPOT)
                .fetchOneInto(Integer.class);
        return epoch != null ? epoch : 0;
    }

    private BigDecimal coalesce(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
