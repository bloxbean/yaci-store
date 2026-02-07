package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model;

import java.math.BigInteger;

public record BFPolicyAsset(String unit, BigInteger quantity, Long slot, String txHash) {
}
