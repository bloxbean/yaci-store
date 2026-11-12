package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CursorServiceImplTimeoutTest {

    @Test
    void withAutoRestartOffANodeThatNeverAnswersFailsTheStartAndKeepsTheCursors() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setAutoRestartEnabled(false);
        storeProperties.setEventPublisherId(1L);
        CursorStorage cursorStorage = mock(CursorStorage.class);
        when(cursorStorage.getCurrentCursor(storeProperties.getEventPublisherId()))
                .thenReturn(Optional.of(new Cursor(1000L, "aa", 10L, "bb", Era.Babbage)));
        BlockFinder blockFinder = new BlockFinder(mock(BlockSync.class)); // connects, then never answers
        blockFinder.answerTimeoutMs = 300;
        CursorServiceImpl cursorService = new CursorServiceImpl(cursorStorage, storeProperties, blockFinder);

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThatThrownBy(cursorService::getStartCursor).isInstanceOf(NodeNotAnsweringException.class));

        // No answer is not "the block is gone": no step back to an older cursor, nothing deleted
        verify(cursorStorage, never()).getPreviousCursor(anyLong(), anyLong());
        verify(cursorStorage, never()).deleteBySlotGreaterThan(anyLong(), anyLong());
    }
}
