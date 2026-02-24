package com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model;

public record BFBlockTxCborRow(
        String txHash,
        byte[] cborData,
        Integer txIndex
) {
}
