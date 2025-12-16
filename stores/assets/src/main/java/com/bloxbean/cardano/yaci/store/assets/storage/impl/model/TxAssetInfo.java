package com.bloxbean.cardano.yaci.store.assets.storage.impl.model;

import java.math.BigInteger;

public interface TxAssetInfo {

    // Aggregated fields
    String getUnit();
    BigInteger getQuantity();

    // Additional fields used for sorting / metadata
    Long getSlot();
    String getTxHash();
}
