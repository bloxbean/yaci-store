package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model;

import java.math.BigInteger;

public record BFAssetHistory(String txHash, String action, BigInteger amount) {
}
