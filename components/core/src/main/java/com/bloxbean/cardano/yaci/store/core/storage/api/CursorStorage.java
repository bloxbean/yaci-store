package com.bloxbean.cardano.yaci.store.core.storage.api;

import com.bloxbean.cardano.yaci.store.common.domain.Cursor;

import java.util.Optional;

public interface CursorStorage {
    void saveCursor(long eventPublisherId, Cursor cursor);
    Optional<Cursor> getCurrentCursor(long eventPublisherId);
    Optional<Cursor> getPreviousCursor(long eventPublisherId, long slot);
    Optional<Cursor> findByBlockHash(long eventPublisherId, String blockHash);
    long deleteBySlotGreaterThan(long eventPublisherId, long slot);

    long deleteCursorBefore(long eventPublisherId, long blockNumber);
}
