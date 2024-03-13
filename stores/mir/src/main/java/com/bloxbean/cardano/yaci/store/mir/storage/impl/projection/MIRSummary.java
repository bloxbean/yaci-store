package com.bloxbean.cardano.yaci.store.mir.storage.impl.projection;

import java.math.BigInteger;

public interface MIRSummary {
    String getTxHash();

    Long getSlot();

    Long getBlockNumber();

    Long getBlockTime();

    String getPot();

    Integer getCertIndex();

    Long getTotalStakeKeys();

    BigInteger getTotalRewards();
}
