package com.bloxbean.cardano.yaci.store.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionStoreProperties {

    @Builder.Default
    private boolean pruningEnabled = false;

    @Builder.Default
    private int pruningInterval = 86400;

    @Builder.Default
    private int pruningSafeSlot = 43200; // 2160 blocks

    /**
     * Enable/disable saving of transaction witnesses.
     * When disabled, transaction witnesses will not be stored in the database.
     * This can help reduce storage requirements if witness data is not needed.
     */
    @Builder.Default
    private boolean saveWitness = false;

    /**
     * Enable/disable saving of transaction CBOR data.
     * When enabled, raw CBOR bytes of transactions will be stored in a separate table.
     * This is useful for transaction verification, multi-party protocols, and trustless validation.
     * Note: This significantly increases storage requirements.
     */
    @Builder.Default
    private boolean saveCbor = false;

    /**
     * Enable/disable pruning of transaction CBOR data.
     * When enabled, CBOR data older than cborRetentionSlots will be automatically deleted.
     */
    @Builder.Default
    private boolean cborPruningEnabled = false;

    /**
     * Retention period for CBOR data in slots.
     * Default: 43,200 slots (based on 2160 safe blocks).
     * CBOR data older than this will be pruned if cborPruningEnabled is true.
     */
    @Builder.Default
    private int cborRetentionSlots = 43200; // 20 * 2160 slots
}
