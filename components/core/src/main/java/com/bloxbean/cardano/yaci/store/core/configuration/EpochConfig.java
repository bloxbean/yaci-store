package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("EpochConfig")
@RequiredArgsConstructor
public class EpochConfig {
    private final GenesisConfig genesisConfig;

    public int epochFromSlot(long shelleyStartSlot, Era era, long slot) {
        if (era == Era.Byron) {
//            return (int) (slot / genesisConfig.slotsPerEpoch(era));
            throw new IllegalArgumentException("epochFromSlot should not be called for Byron era. " +
                    "Byron era block already has epoch info");
        } else {
            long shelleyStartEpoch = shelleyStartSlot / genesisConfig.slotsPerEpoch(Era.Byron);
            long epochsAfterShelley = (slot - shelleyStartSlot) / genesisConfig.slotsPerEpoch(Era.Shelley);
            return (int) (shelleyStartEpoch + epochsAfterShelley);
        }
    }

    public int shelleyEpochSlot(long shelleyStartSlot, long absoluteSlot) {
        int epoch = epochFromSlot(shelleyStartSlot, Era.Shelley, absoluteSlot);
        long shelleyStartEpoch = shelleyStartSlot / genesisConfig.slotsPerEpoch(Era.Byron);

        long epochsFroShelleyStart = epoch - shelleyStartEpoch;
        long currentEpochStartSlot = epochsFroShelleyStart * genesisConfig.slotsPerEpoch(Era.Shelley);

        long epochSlot = absoluteSlot - (currentEpochStartSlot + shelleyStartSlot);

        return (int) epochSlot;
    }

    public long epochSlotToAbsoluteSlot(long shelleyStartSlot, int epoch, int epochSlot) {
        long shelleyStartEpoch = shelleyStartSlot / genesisConfig.slotsPerEpoch(Era.Byron);
        long epochsAfterShelley = epoch - shelleyStartEpoch;
        long currentEpochStartSlot = epochsAfterShelley * genesisConfig.slotsPerEpoch(Era.Shelley);

        return currentEpochStartSlot + shelleyStartSlot + epochSlot;
    }
}
