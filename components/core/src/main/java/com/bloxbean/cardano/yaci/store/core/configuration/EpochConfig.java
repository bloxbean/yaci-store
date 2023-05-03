package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("EpochConfig")
@RequiredArgsConstructor
public class EpochConfig {
    private final Long shellyEpochLength = 432000L;
    private final Long shellySlotLength = 1L;

    private final Long mainnetShellyKnownSlot = 4492800L;
    private final Long legacyTestnetShellyKnownSlot = 1598400L;
    private final Long preprodShellyKnownSlot = 86400L;
    private final Long previewShellyKnownSlot = 20L;

    private final GenesisConfig genesisConfig;

    //TODO -- Get this during crawling from the network
    public Long getShelleyKnownSlot(long protocolMagic) {
        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);

        if (networkType == NetworkType.MAINNET) {
            return mainnetShellyKnownSlot;
        } else if (networkType == NetworkType.LEGACY_TESTNET) {
            return legacyTestnetShellyKnownSlot;
        } else if (networkType == NetworkType.PREPROD) {
            return preprodShellyKnownSlot;
        } else if (networkType == NetworkType.PREVIEW) {
            return previewShellyKnownSlot;
        }

        return null;
    }

    public Long getShellyEpochLength() {
        return shellyEpochLength;
    }

    public Long getShellySlotLength() {
        return shellySlotLength;
    }

    public int epochFromSlot(long protocolMagic, Era era, long slot) {
        Long shelleyStartSlot = getShelleyKnownSlot(protocolMagic);

        if (era == Era.Byron) {
            return (int) (slot / genesisConfig.slotsPerEpoch(era));
        } else {
            long shelleyStartEpoch = shelleyStartSlot / genesisConfig.slotsPerEpoch(Era.Byron);
            long epochsAfterShelley = (slot - shelleyStartSlot) / genesisConfig.slotsPerEpoch(Era.Shelley);
            return (int) (shelleyStartEpoch + epochsAfterShelley);
        }

    }
}
