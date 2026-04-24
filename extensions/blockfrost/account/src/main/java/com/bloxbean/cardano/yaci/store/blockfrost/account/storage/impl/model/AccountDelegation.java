package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;

public record AccountDelegation(
        int activeEpoch,
        String txHash,
        BigInteger amount,
        String poolId,
        Long txSlot,
        Long blockTime,
        Long blockHeight
) {}
