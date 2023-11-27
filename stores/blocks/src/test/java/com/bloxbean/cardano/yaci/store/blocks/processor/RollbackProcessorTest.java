package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.RollbackStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RollbackProcessorTest {
    @Mock
    private RollbackStorage rollbackStorage;

    @InjectMocks
    private RollbackProcessor rollbackProcessor;

    @Test
    void givenRollbackEvent_shouldSaveRollbackEvent() {
        rollbackProcessor.handleRollbackEvent(RollbackEvent.builder()
                .rollbackTo(new Point(86880, "d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937"))
                .currentPoint(new Point(90000, "d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937"))
                .build());

        Mockito.verify(rollbackStorage, Mockito.times(1)).save(Mockito.any());
    }
}
