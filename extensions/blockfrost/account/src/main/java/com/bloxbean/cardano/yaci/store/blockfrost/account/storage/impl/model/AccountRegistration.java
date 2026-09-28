package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;

public record AccountRegistration(
        String txHash,
        String type,
        Long txSlot,
        Long blockTime,
        Long blockHeight,
        BigInteger deposit
) {}
