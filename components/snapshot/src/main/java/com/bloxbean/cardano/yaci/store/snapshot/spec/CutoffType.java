package com.bloxbean.cardano.yaci.store.snapshot.spec;

/**
 * Table-specific predicate that removes rows beyond the consistency point. There is deliberately no
 * generic rule inferred from a column merely named {@code slot} or {@code epoch}.
 */
public enum CutoffType {
    /** No filter. Only valid for tables whose full contents are always at or before the point. */
    NONE,
    /** {@code column <= cutSlot}. */
    SLOT_LTE,
    /** {@code column <= completedEpoch}. */
    EPOCH_LTE,
    /** {@code column <= completedEpoch - offset}. */
    EPOCH_LTE_OFFSET
}
