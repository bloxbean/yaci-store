package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;

public record AccountReward(
        int epoch,
        BigInteger amount,
        String poolId,
        String type
) {}
