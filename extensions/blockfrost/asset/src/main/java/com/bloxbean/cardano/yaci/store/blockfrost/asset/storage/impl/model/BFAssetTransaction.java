package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model;

public record BFAssetTransaction(String txHash, Long txIndex, Long blockHeight, Long blockTime) {
}
