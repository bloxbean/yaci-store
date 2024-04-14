package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class CursorCleanupScheduler {
    private final CursorStorage cursorStorage;
    private final StoreProperties storeProperties;

    @Transactional
    @Scheduled(fixedRateString = "#{storeProperties.cursorCleanupInterval * 1000}", initialDelay = 30000)
    public void scheduleCursorCleanup() {
        log.info("Running cursor cleanup scheduler");
        Optional<Cursor> cursor = cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId());
        log.info("Current cursor: {}", cursor);

        if (cursor.isEmpty())
            return;

        //Block no to delete from
        long fromBlock = cursor.get().getBlock() - storeProperties.getCursorNoOfBlocksToKeep();
        if (fromBlock < 0)
            return;

        int blocks = cursorStorage.deleteCursorBefore(storeProperties.getEventPublisherId(), fromBlock);
        log.info("Deleted {} cursors before block {}", blocks, fromBlock);
    }
}
