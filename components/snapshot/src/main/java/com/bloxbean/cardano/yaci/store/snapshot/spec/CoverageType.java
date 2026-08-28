package com.bloxbean.cardano.yaci.store.snapshot.spec;

/**
 * Assertion evaluated after the consistency point is chosen, so a table that stops short of the
 * point fails the export instead of silently producing a short snapshot.
 */
public enum CoverageType {
    NONE,
    /** {@code max(column) >= cutSlot}. */
    SLOT_GTE_CUT,
    /** {@code max(column) >= completedEpoch}. */
    EPOCH_GTE_COMPLETED
}
