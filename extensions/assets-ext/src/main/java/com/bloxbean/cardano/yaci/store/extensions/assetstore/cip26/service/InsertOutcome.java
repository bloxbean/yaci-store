package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

/**
 * Result of attempting to persist a single CIP-26 entry.
 *
 * <p>The sync service uses these to decide whether to advance the
 * {@code last_commit_hash} cursor: any {@link #TRANSIENTLY_FAILED} in a batch
 * blocks the advance so the next sync retries; otherwise the hash advances
 * and {@link #PERMANENTLY_SKIPPED} entries are documented as dropped.
 */
public enum InsertOutcome {

    /** Entry was successfully persisted. */
    INSERTED,

    /**
     * Entry will never persist as-is — either it failed CIP-26 validation
     * (yaci-side pre-check or upstream library) or the database rejected it
     * with a non-transient error (constraint violation, value too long,
     * encoding issue). Retrying the same entry won't help; advance past it.
     */
    PERMANENTLY_SKIPPED,

    /**
     * Save failed with what looks like a recoverable database condition
     * (lock timeout, lost connection, deadlock) or an exception we don't
     * recognise. The cursor must NOT advance — the next sync should retry
     * this entry. Genuine recurring failures will surface as repeating log
     * lines and need investigation.
     */
    TRANSIENTLY_FAILED
}
