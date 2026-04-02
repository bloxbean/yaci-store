package com.bloxbean.cardano.yaci.store.blockfrost.network.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.network.storage.BFNetworkStorageReader;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.ADAPOT;
import static org.jooq.impl.DSL.max;

/**
 * JOOQ-based implementation of {@link BFNetworkStorageReader}.
 * <p>
 * Queries the UTxO set, rewards, withdrawals and epoch-stake tables to compute
 * values needed by the Blockfrost {@code /network} endpoint.
 */
@Component
@RequiredArgsConstructor
public class BFNetworkStorageReaderImpl implements BFNetworkStorageReader {

    private final DSLContext dsl;

    // TODO: impl
    @Override
    public BigInteger getLockedSupply() {
        // TODO: Add an {@code is_script} boolean column to {@code address_utxo} during indexing
        // and implement with a direct column filter instead of bech32 prefix matching.
        return BigInteger.ZERO;
    }

    // TODO: impl
    @Override
    public BigInteger getCirculatingSupply(int currentEpoch) {
        return BigInteger.ZERO;
    }

    // TODO: impl
    @Override
    public BigInteger getLiveStake() {
        // TODO: Implement the full real-time live stake CTE (delegated accounts × current UTxOs +
        // spendable rewards − withdrawals) behind a feature flag for installations that need
        // exact Blockfrost parity on this field.
        return BigInteger.ZERO;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the current epoch number from the latest adapot row.
     */
    @Override
    public int getCurrentEpoch() {
        Integer epoch = dsl.select(max(ADAPOT.EPOCH))
                .from(ADAPOT)
                .fetchOneInto(Integer.class);
        return epoch != null ? epoch : 0;
    }
}
