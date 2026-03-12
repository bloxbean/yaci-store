package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

public record AccountTransaction(
        String address,
        String txHash,
        long txIndex,
        long blockHeight,
        long blockTime
) {}
