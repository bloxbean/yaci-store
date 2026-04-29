package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;

public record AccountInfo(
        String stakeAddress,
        boolean active,
        boolean registered,
        Integer activeEpoch,
        BigInteger controlledAmount,
        BigInteger rewardsSum,
        BigInteger withdrawalsSum,
        BigInteger reservesSum,
        BigInteger treasurySum,
        String poolId,
        String drepId
) {}
