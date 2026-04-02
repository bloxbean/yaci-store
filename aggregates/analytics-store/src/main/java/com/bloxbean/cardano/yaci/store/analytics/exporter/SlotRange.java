package com.bloxbean.cardano.yaci.store.analytics.exporter;

/**
 * Represents a range of blockchain slots [startSlot, endSlot).
 *
 * Used for querying blockchain data within a specific slot range.
 * The range is half-open: [startSlot, endSlot), meaning:
 * - startSlot is INCLUDED
 * - endSlot is EXCLUDED
 *
 * This aligns with SQL WHERE clauses: WHERE slot &gt;= startSlot AND slot &lt; endSlot
 *
 * Note: startSlot may be -1 to include genesis data (slot=-1) in the range.
 *
 * @param startSlot Starting slot (inclusive), minimum -1 for genesis data
 * @param endSlot Ending slot (exclusive)
 */
public record SlotRange(long startSlot, long endSlot) {

    /**
     * Validate that the slot range is valid.
     */
    public SlotRange {
        if (startSlot < -1) {
            throw new IllegalArgumentException("Start slot cannot be less than -1: " + startSlot);
        }
        if (endSlot < 0) {
            throw new IllegalArgumentException("End slot cannot be negative: " + endSlot);
        }
        if (endSlot <= startSlot) {
            throw new IllegalArgumentException(
                    "End slot must be greater than start slot: [" + startSlot + ", " + endSlot + ")");
        }
    }

    /**
     * Get the number of slots in this range.
     *
     * @return Number of slots
     */
    public long getSlotCount() {
        return endSlot - startSlot;
    }
}
