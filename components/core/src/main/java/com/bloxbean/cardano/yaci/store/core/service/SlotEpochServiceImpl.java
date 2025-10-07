package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.time.SlotEpochService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlotEpochServiceImpl implements SlotEpochService {
    private final EraService eraService;

    @Override
    public long slotsPerEpoch(Era era) {
        return eraService.slotsPerEpoch(era);
    }

    @Override
    public long getFirstNonByronSlot() {
        return eraService.getFirstNonByronSlot();
    }

    @Override
    public int getEpochNo(Era era, long absoluteSlot) {
        return eraService.getEpochNo(era, absoluteSlot);
    }

    @Override
    public long getShelleyAbsoluteSlot(int epoch, int epochSlot) {
        return eraService.getShelleyAbsoluteSlot(epoch, epochSlot);
    }
}


