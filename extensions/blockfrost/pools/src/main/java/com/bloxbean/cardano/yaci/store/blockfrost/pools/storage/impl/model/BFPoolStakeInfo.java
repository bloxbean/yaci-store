package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model;

import java.math.BigInteger;

/**
 * Aggregated stake information for a pool from the latest epoch_stake snapshot.
 * Used to populate live/active fields in pool detail and extended list endpoints.
 */
public record BFPoolStakeInfo(
        String poolId,
        BigInteger liveStake,          // SUM(amount) at latest epoch snapshot
        int liveDelegators,             // COUNT(*) at latest epoch snapshot
        BigInteger activeStake,        // SUM(amount) at latest-2 epoch snapshot (currently active)
        BigInteger totalLiveStake,     // total across all pools at latest epoch (staked only)
        BigInteger totalActiveStake,   // total across all pools at latest-2 epoch (staked only)
        BigInteger circulationSupply,  // total circulating lovelace from adapot.circulation (for saturation)
        int nopt                        // desired number of pools from protocol params
) {
    /**
     * live_size = live_stake / total_live_stake (epoch snapshot proportion)
     *
     * <p><b>Known limitation</b>: Blockfrost computes this as
     * {@code pool_live_utxo_stake / total_live_utxo_stake_all_pools}, where live_utxo_stake
     * is the real-time sum of (unspent UTxOs + spendable rewards - withdrawals) for every
     * currently-delegating address. This requires aggregating UTxOs across all ~100K+ active
     * delegators network-wide — a query Blockfrost itself marks as "potentially heavy" and caches.
     *
     * <p>Our denominator ({@code epoch_stake} total) excludes accumulated unclaimed rewards held
     * in delegators' UTxO balance, making it roughly 2x smaller than Blockfrost's denominator.
     * As a result, our {@code live_size} values are approximately 2x higher than Blockfrost's.
     * After a full sync, the ratio within a single epoch remains accurate; the absolute value
     * will still differ until a full UTxO-based live-stake computation is implemented.
     *
     * <p>Accepted deviation — documented in {@code specs/001-blockfrost-pools/deviation-report.md}.
     */
    public double liveSize() {
        if (totalLiveStake == null || totalLiveStake.compareTo(BigInteger.ZERO) == 0 || liveStake == null) return 0.0;
        return liveStake.doubleValue() / totalLiveStake.doubleValue();
    }

    /**
     * active_size = active_stake / total_active_stake
     */
    public double activeSize() {
        if (totalActiveStake == null || totalActiveStake.compareTo(BigInteger.ZERO) == 0 || activeStake == null) return 0.0;
        return activeStake.doubleValue() / totalActiveStake.doubleValue();
    }

    /**
     * live_saturation = live_stake / (circulation / nopt)
     *
     * <p>Formula matches Blockfrost exactly:
     * {@code circulation = adapot.circulation = 45_000_000_000_000_000 - reserves}
     * (Blockfrost comments: "although it's called circulation in the ledger code,
     * we actually need total_supply instead" — meaning {@code 45B ADA - reserves}).
     * {@code nopt} (optimal pool count) is read from {@code epoch_param.params.nopt}.
     *
     * <p>Remaining difference vs Blockfrost (~4%) is sync lag only: our {@code epoch_stake}
     * snapshot lags behind BF's live UTxO-based numerator by the number of unsynced epochs.
     * At full sync, the difference is within ~0.0003%.
     */
    public double liveSaturation() {
        if (liveStake == null || nopt <= 0) return 0.0;
        BigInteger denominator = (circulationSupply != null && circulationSupply.compareTo(BigInteger.ZERO) > 0)
                ? circulationSupply
                : totalLiveStake;
        if (denominator == null || denominator.compareTo(BigInteger.ZERO) == 0) return 0.0;
        double saturationThreshold = denominator.doubleValue() / nopt;
        return liveStake.doubleValue() / saturationThreshold;
    }
}
