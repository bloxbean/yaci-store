package com.bloxbean.cardano.yaci.store.common.domain;

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
}
