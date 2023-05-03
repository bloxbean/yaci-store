package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import org.springframework.stereotype.Component;

@Component
public class GenesisConfig {

    private final long mainnetStartTime = 1_506_203_091;
    private final long testnetStartTime = 1_564_020_236;
    private final long preprodStartTime = 1_654_041_600;
    private final long previewStartTime = 1_666_656_000;

    public long slotDuration(Era era) {
        if (era == Era.Byron)
            return 20; //20 sec
        else
            return 1; //1 sec
    }

    public long slotsPerEpoch(Era era) {
        long totalSecsIn5DaysEpoch = 432000;
        return totalSecsIn5DaysEpoch / slotDuration(era);
    }

    public long getStartTime(long protocolMagic) {
        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);
        if (networkType == NetworkType.MAINNET) {
            return mainnetStartTime;
        } else if (networkType == NetworkType.LEGACY_TESTNET) {
            return testnetStartTime;
        } else if (networkType == NetworkType.PREPROD) {
            return preprodStartTime;
        } else if (networkType == NetworkType.PREVIEW) {
            return previewStartTime;
        }

        return 0;
    }

    public long absoluteSlot(Era era, long epoch, long slotInEpoch) {
        return (slotsPerEpoch(era) * epoch) + slotInEpoch;
    }


}
