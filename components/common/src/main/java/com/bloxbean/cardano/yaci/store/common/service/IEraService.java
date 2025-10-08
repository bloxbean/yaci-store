package com.bloxbean.cardano.yaci.store.common.service;

import com.bloxbean.cardano.yaci.core.model.Era;

public interface IEraService {
    long slotsPerEpoch(Era era);
    long getFirstNonByronSlot();
    int getEpochNo(Era era, long absoluteSlot);
    long getShelleyAbsoluteSlot(int epoch, int epochSlot);
    Era getEraForEpoch(int epoch);
}
