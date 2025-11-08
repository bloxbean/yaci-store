package com.bloxbean.cardano.yaci.store.blocks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlocksStoreProperties {
    private boolean enabled;

    private boolean metricsEnabled = true;
    private long metricsUpdateInterval = 60000; // 60 seconds

    /**
     * Enable/disable saving of block CBOR data.
     * When enabled, raw CBOR bytes of blocks will be stored in a separate table.
     * This is useful for block verification and debugging.
     * Note: This significantly increases storage requirements.
     */
    @Builder.Default
    private boolean saveCbor = false;

    /**
     * Enable/disable pruning of block CBOR data.
     * When enabled, CBOR data older than cborRetentionSlots will be automatically deleted.
     */
    @Builder.Default
    private boolean cborPruningEnabled = false;

    /**
     * Retention period for CBOR data in slots.
     * Default: 2,592,000 slots = 30 days (1 slot = 1 second on Cardano mainnet).
     * CBOR data older than this will be pruned if cborPruningEnabled is true.
     */
    @Builder.Default
    private int cborRetentionSlots = 2592000; // 30 days (30 * 24 * 60 * 60 slots)
}
