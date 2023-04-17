package com.bloxbean.cardano.yaci.store.core.storage.api;

import com.bloxbean.cardano.yaci.store.core.domain.Cursor;

import java.util.Optional;

public interface CursorStorage {
    void saveCursor(long eventPublisherId, Cursor cursor);
    Optional<Cursor> getCursorAtCurrentMinusOffset(long eventPublisherId, int offset);
    Optional<Cursor> getCurrentCursor(long eventPublisherId);
    Optional<Cursor> findByBlockHash(long eventPublisherId, String blockHash);
    int deleteBySlotGreaterThan(long eventPublisherId, long slot);
}
