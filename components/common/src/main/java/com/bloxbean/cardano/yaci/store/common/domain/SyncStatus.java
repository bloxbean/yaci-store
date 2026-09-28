package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SyncStatus(
        long block,
        long slot,
        int epoch,
        String era,
        String blockHash,
        double syncPercentage,
        long networkBlock,
        long networkSlot,
        boolean synced,
        long protocolMagic
) {
    public static final long UNKNOWN_NETWORK_TIP = -1L;

    @JsonProperty
    public boolean networkTipAvailable() {
        return networkBlock != UNKNOWN_NETWORK_TIP && networkSlot != UNKNOWN_NETWORK_TIP;
    }
}
