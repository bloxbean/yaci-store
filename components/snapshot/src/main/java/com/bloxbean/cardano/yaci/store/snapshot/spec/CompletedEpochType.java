package com.bloxbean.cardano.yaci.store.snapshot.spec;

/**
 * How a table constrains the newest selectable completed epoch.
 *
 * <p>Only tables that are guaranteed to produce data for every epoch may gate the point. Sparse
 * tables (a committee change, a pool retirement) declare {@link #NONE} so an epoch without activity
 * cannot drag the snapshot backwards.
 */
public enum CompletedEpochType {
    /** Does not gate the consistency point. */
    NONE,
    /** Supports completed epoch {@code max(column)}. */
    MAX_EPOCH,
    /** Supports completed epoch {@code max(column) + offset}, with a signed offset. */
    MAX_EPOCH_OFFSET
}
