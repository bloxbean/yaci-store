package com.bloxbean.cardano.yaci.store.snapshot.spec;

/** Smallest unit of work that may be committed independently during import. */
public enum BatchBoundary {
    /** A bounded set of Parquet files. */
    FILE_SET,
    /**
     * All files of one partition directory. Required whenever a transform groups rows, because a
     * grouped key could otherwise be emitted by more than one batch (see {@code address_utxo}).
     */
    WHOLE_PARTITION,
    /** The whole table in one batch. Used for small tables. */
    SINGLE_BATCH
}
