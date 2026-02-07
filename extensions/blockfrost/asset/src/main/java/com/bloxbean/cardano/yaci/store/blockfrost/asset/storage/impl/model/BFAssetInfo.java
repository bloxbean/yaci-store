package com.bloxbean.cardano.yaci.store.blockfrost.asset.storage.impl.model;

import java.math.BigInteger;

public record BFAssetInfo(String unit,
                          String policyId,
                          String assetName,
                          String fingerprint,
                          BigInteger quantity,
                          String initialMintTxHash,
                          long mintOrBurnCount) {
}
