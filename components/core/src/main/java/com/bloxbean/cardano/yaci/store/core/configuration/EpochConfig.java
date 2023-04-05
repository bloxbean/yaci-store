package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import org.springframework.stereotype.Component;

@Component("EpochConfig")
public class EpochConfig {
    private final Long shellyEpochLength = 432000L;
    private final Long shellySlotLength = 1L;

    private final Long mainnetShellyKnownSlot = 4492800L;
    private final Long legacyTestnetShellyKnownSlot = 1598400L;
    private final Long preprodShellyKnownSlot = 86400L;
    private final Long previewShellyKnownSlot = 20L;


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
}
