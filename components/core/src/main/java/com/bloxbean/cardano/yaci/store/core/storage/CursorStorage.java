package com.bloxbean.cardano.yaci.store.core.storage;

import com.bloxbean.cardano.yaci.store.core.domain.Cursor;

import java.util.Optional;

public interface CursorStorage {

    void saveCursor(Long eventPublisherId, Cursor cursor);

    Optional<Cursor> getCurrentCursor(Long eventPublisherId);

    Optional<Cursor> getPreviousCursor(Long eventPublisherId, Long slot);

    Optional<Cursor> findByBlockHash(Long eventPublisherId, String blockHash);

    Integer deleteBySlotGreaterThan(Long eventPublisherId, Long slot);

    Integer deleteCursorBefore(Long eventPublisherId, Long blockNumber);
}
