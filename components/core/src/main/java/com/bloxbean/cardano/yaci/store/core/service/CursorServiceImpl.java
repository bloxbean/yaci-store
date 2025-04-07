package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ReadOnly(false)
@Slf4j
public class CursorServiceImpl implements CursorService {
    private final CursorStorage cursorStorage;
    private final StoreProperties storeProperties;
    private final BlockFinder blockFinder;

    private AtomicLong lastBlock = new AtomicLong(0);
    private Instant lastProcessedTime = Instant.now();
    private boolean syncMode;

    public CursorServiceImpl(CursorStorage cursorStorage, StoreProperties storeProperties, BlockFinder blockFinder) {
        this.cursorStorage = cursorStorage;
        this.storeProperties = storeProperties;
        this.blockFinder = blockFinder;
    }

    @Override
    public void setCursor(Cursor cursor) {
        if (cursor.getBlockHash() == null)
            throw new RuntimeException("BlockHash can't be null.");

        cursorStorage.saveCursor(storeProperties.getEventPublisherId(), cursor);
        printLog(cursor.getBlock(), cursor.getEra());
    }

    @Override
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

    @Override
    public Optional<Cursor> getCursor() {
        return cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId());
    }

    @Override
    public Optional<Cursor> getPreviousCursor(long slot) {
        return cursorStorage.getPreviousCursor(storeProperties.getEventPublisherId(), slot);
    }

    @Override
    public Optional<Cursor> getCursorByBlockHash(String blockHash) {
        return cursorStorage.findByBlockHash(storeProperties.getEventPublisherId(), blockHash);
    }

    @Transactional
    @Override
    public void rollback(long slot) {
        log.info("Rollback cursor_ to slot : " + slot);
        long count = cursorStorage.deleteBySlotGreaterThan(storeProperties.getEventPublisherId(), slot);
        log.info("Rollback -- {} cursor records", count);

        Cursor cursor
                = cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId()).orElse(new Cursor());
        log.info("Cursor : Slot=" + cursor.getSlot() + ", Hash=" + cursor.getBlockHash());
    }

    @Override
    public boolean isSyncMode() {
        return syncMode;
    }

    @Override
    public void setSyncMode(boolean syncMode) {
        this.syncMode = syncMode;
    }

    private void printLog(long block, Era era) {
        if (lastBlock.get() == 0) {
            lastBlock.set(block);
            return;
        }

        var diff = block - lastBlock.get();
        if (diff < 0) {
            return;
        }

        if (!syncMode) {
            if (diff >= 100) {
                Instant now = Instant.now();
                log.info("# of blocks written: {}, Time taken: {} ms", diff, Duration.between(lastProcessedTime, now).toMillis());
                log.info("Block No: " + block + "  , Era: " + era);
                lastBlock.set(block);
                lastProcessedTime = now;
            }

        } else {
            log.info("# of blocks written: " + diff);
            log.info("Block No: " + block);
            lastBlock.set(block);
        }
    }
}
