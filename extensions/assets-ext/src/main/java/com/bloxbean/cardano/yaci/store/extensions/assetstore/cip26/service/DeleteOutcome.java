package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

/**
 * Result of attempting to delete a single CIP-26 entry whose mapping file was
 * removed from the upstream registry.
 *
 * <p>Same cursor semantics as {@link InsertOutcome}: any
 * {@link #TRANSIENTLY_FAILED} in a batch blocks the {@code last_commit_hash}
 * advance so the next sync retries; {@link #PERMANENTLY_FAILED} entries are
 * documented as stuck and skipped past.
 */
public enum DeleteOutcome {

    /** Row was deleted (or was already absent — deletion is idempotent). */
    DELETED,

    /**
     * The database rejected the delete with a non-transient error (e.g. a
     * foreign-key reference from another table). Retrying the same delete
     * won't help; advance past it and log loudly so operators can intervene.
     */
    PERMANENTLY_FAILED,

    /**
     * Delete failed with what looks like a recoverable database condition
     * (lock timeout, lost connection, deadlock) or an exception we don't
     * recognise. The cursor must NOT advance — the next sync retries.
     */
    TRANSIENTLY_FAILED
}
