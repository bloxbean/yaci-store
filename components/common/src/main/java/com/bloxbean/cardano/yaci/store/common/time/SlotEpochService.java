package com.bloxbean.cardano.yaci.store.common.time;

import com.bloxbean.cardano.yaci.core.model.Era;

public interface SlotEpochService {
    long slotsPerEpoch(Era era);
    long getFirstNonByronSlot();
    int getEpochNo(Era era, long absoluteSlot);
    long getShelleyAbsoluteSlot(int epoch, int epochSlot);
}


