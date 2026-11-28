package com.bloxbean.cardano.yaci.store.snapshot.spec;

/**
 * How a target table is restored. Every table created by Flyway must carry exactly one of these,
 * so a new migration cannot leave a table silently unclassified.
 */
public enum RestoreMode {
    /** Loaded from packaged Parquet through the declarative importer. */
    IMPORT,
    /** Rebuilt by a named {@code SnapshotHandler} (control state such as {@code cursor_}). */
    HANDLER,
    /** Proven empty at export time for the supported profile; the importer leaves it empty. */
    EMPTY_EXPECTED,
    /** Repopulated by the application itself after restart (live/local caches). */
    RUNTIME_REBUILT,
    /** Declared limitation: no source data exists. Recorded in the manifest and the import report. */
    NOT_RESTORED
}
