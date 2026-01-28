package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.model;

import java.math.BigInteger;

public interface BFEpochSumProjection {
    Integer getEpoch();
    BigInteger getTotal();
}
