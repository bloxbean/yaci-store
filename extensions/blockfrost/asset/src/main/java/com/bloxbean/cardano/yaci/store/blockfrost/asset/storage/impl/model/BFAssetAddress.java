package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model;

import java.math.BigInteger;

public record BFAssetAddress(String address, BigInteger quantity, Long firstSeenSlot) {
}
