package com.bloxbean.cardano.yaci.store.common.service;

import com.bloxbean.cardano.yaci.store.common.domain.Cursor;

import java.util.Optional;

public interface CursorService {
    void setCursor(Cursor cursor);
    Optional<Cursor> getStartCursor();
    Optional<Cursor> getCursor();
    Optional<Cursor> getPreviousCursor(long slot);
    Optional<Cursor> getCursorByBlockHash(String blockHash);
    void rollback(long slot);
    void setSyncMode(boolean syncMode);
    boolean isSyncMode();
}
