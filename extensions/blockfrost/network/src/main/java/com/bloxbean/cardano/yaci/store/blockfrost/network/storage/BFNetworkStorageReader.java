package com.bloxbean.cardano.yaci.store.blockfrost.network.storage;

import java.math.BigInteger;

/**
 * Storage reader for Blockfrost network endpoint queries that require
 * cross-store data (UTxO set, epoch stake, rewards, withdrawals).
 */
public interface BFNetworkStorageReader {

    /**
     * Sum of lovelace locked in unspent UTxOs at script payment-credential addresses.
     */
    BigInteger getLockedSupply();

    /**
     * Circulating supply following the Blockfrost formula:
     * {@code SUM(unspent UTxO) + SUM(spendable rewards) + SUM(spendable reward_rest) - SUM(withdrawals)}
     *
     * @param currentEpoch the current epoch number (for spendable_epoch filtering)
     */
    BigInteger getCirculatingSupply(int currentEpoch);

    /**
     * Live stake: total lovelace delegated to pools based on the latest completed
     * epoch-stake snapshot.
     */
    BigInteger getLiveStake();

    /**
     * Returns the current epoch number from the adapot table.
     */
    int getCurrentEpoch();
}
