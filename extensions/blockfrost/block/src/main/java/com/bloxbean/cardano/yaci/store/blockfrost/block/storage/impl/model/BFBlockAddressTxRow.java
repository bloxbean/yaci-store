package com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model;

public record BFBlockAddressTxRow(
        String address,
        String txHash,
        Integer txIndex
) {
}
