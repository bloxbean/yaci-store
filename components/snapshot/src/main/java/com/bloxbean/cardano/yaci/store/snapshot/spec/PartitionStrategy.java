package com.bloxbean.cardano.yaci.store.snapshot.spec;

/**
 * Mirrors the analytics exporter partitioning of the source relation. Declared here rather than
 * imported so the snapshot component stays independent of analytics-store.
 */
public enum PartitionStrategy {
    DAILY,
    EPOCH,
    NONE
}
