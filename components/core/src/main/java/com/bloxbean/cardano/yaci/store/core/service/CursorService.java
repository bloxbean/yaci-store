package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class CursorService {
    private final CursorStorage cursorStorage;
    private final StoreProperties storeProperties;
    private final BlockFinder blockFinder;

    private AtomicLong count;
    private boolean syncMode;

    public CursorService(CursorStorage cursorStorage, StoreProperties storeProperties, BlockFinder blockFinder) {
        this.cursorStorage = cursorStorage;
        this.storeProperties = storeProperties;
        this.blockFinder = blockFinder;
        this.count = new AtomicLong(0);
    }

    public void setCursor(Cursor cursor) {
        if (cursor.getBlockHash() == null)
            throw new RuntimeException("BlockHash can't be null.");

        cursorStorage.saveCursor(storeProperties.getEventPublisherId(), cursor);
        printLog(cursor.getBlock(), cursor.getEra());
    }

    public Optional<Cursor> getStartCursor() {
        Cursor _currentCursor = null;

        while (true) {
            Optional<Cursor> currentCursor;
            if (_currentCursor == null) {
                currentCursor = cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId());
            } else {
                currentCursor = cursorStorage.getPreviousCursor(storeProperties.getEventPublisherId(), _currentCursor.getSlot());
            }

            if (currentCursor.isPresent()) {
                //check if cursor exists
                boolean blockExists = blockFinder.blockExists(new Point(currentCursor.get().getSlot(), currentCursor.get().getBlockHash()));
                if (blockExists) {
                    log.info("Valid block. slot:" + currentCursor.get().getSlot() + ", blockHash: " + currentCursor.get().getBlockHash()
                            + ", block : " + currentCursor.get().getBlock());

                    //Delete all invalid cursors
                    cursorStorage.deleteBySlotGreaterThan(storeProperties.getEventPublisherId(), currentCursor.get().getSlot());
                    return currentCursor;
                } else {
                    log.info("InValid block. slot:" + currentCursor.get().getSlot() + ", blockHash: " + currentCursor.get().getBlockHash()
                            + ", block : " + currentCursor.get().getBlock());
                    _currentCursor = currentCursor.get();
                }
            } else {
                log.info("No cursor found.");
                return Optional.empty();
            }

        }
    }

    public Optional<Cursor> getCursor() {
        return cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId());
    }

    public Optional<Cursor> getPreviousCursor(long slot) {
        return cursorStorage.getPreviousCursor(storeProperties.getEventPublisherId(), slot);
    }

    public Optional<Cursor> getCursorByBlockHash(String blockHash) {
        return cursorStorage.findByBlockHash(storeProperties.getEventPublisherId(), blockHash);
    }

    @Transactional
    public void rollback(long slot) {
        log.info("Rollback cursor_ to slot : " + slot);
        int count = cursorStorage.deleteBySlotGreaterThan(storeProperties.getEventPublisherId(), slot);
        log.info("Rollback -- {} cursor records", count);

        Cursor cursor
                = cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId()).orElse(new Cursor());
        log.info("Cursor : Slot=" + cursor.getSlot() + ", Hash=" + cursor.getBlockHash());
    }

    public boolean isSyncMode() {
        return syncMode;
    }

    public void setSyncMode(boolean syncMode) {
        this.syncMode = syncMode;
    }

    private void printLog(long block, Era era) {
        count.incrementAndGet();
        double val = count.get() % 100;

        if (!syncMode) {
            if (val == 0) {
                log.info("# of blocks written: " + count.get());
                log.info("Block No: " + block + "  , Era: " + era);
            }

        } else {
            log.info("# of blocks written: " + count.get());
            log.info("Block No: " + block);
        }
    }
}
