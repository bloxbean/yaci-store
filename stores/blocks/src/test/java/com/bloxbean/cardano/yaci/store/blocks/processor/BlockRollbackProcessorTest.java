package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockRollbackProcessorTest {
    @Mock
    private BlockStorage blockStorage;
    @InjectMocks
    private BlockProcessor blockProcessor;

    @Test
    void handleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(86880, "d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937"))
                .currentPoint(new Point(90000, "d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937"))
                .build();

        blockProcessor.handleRollbackEvent(rollbackEvent);
        Mockito.verify(blockStorage, Mockito.times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }
}
