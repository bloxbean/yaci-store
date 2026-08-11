package com.bloxbean.cardano.yaci.store.api.core.dto;

import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

/**
 * Indexer sync status : store tip vs node tip.
 */
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SyncStatusDto(
        long block,
        long slot,
        int epoch,
        String era,
        String blockHash,
        Double syncPercentage,
        Long networkBlock,
        Long networkSlot,
        Long lagBlocks,
        boolean networkTipAvailable,
        boolean synced,
        long protocolMagic
) {
    public static SyncStatusDto from(SyncStatus syncStatus) {
        boolean networkTipAvailable = syncStatus.networkTipAvailable();

        return SyncStatusDto.builder()
                .block(syncStatus.block())
                .slot(syncStatus.slot())
                .epoch(syncStatus.epoch())
                .era(syncStatus.era())
                .blockHash(syncStatus.blockHash())
                .syncPercentage(networkTipAvailable ? syncStatus.syncPercentage() : null)
                .networkBlock(networkTipAvailable ? syncStatus.networkBlock() : null)
                .networkSlot(networkTipAvailable ? syncStatus.networkSlot() : null)
                .lagBlocks(networkTipAvailable ? Math.max(0, syncStatus.networkBlock() - syncStatus.block()) : null)
                .networkTipAvailable(networkTipAvailable)
                .synced(networkTipAvailable && syncStatus.synced())
                .protocolMagic(syncStatus.protocolMagic())
                .build();
    }
}
