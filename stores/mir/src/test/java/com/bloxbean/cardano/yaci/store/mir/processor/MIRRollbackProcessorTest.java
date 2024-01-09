package com.bloxbean.cardano.yaci.store.mir.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MIRRollbackProcessorTest {
    @Mock
    private MIRStorage mirStorage;

    @InjectMocks
    private MIRProcessor mirProcessor;

    @Test
    void handleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        mirProcessor.handleRollbackEvent(rollbackEvent);

        Mockito.verify(mirStorage, Mockito.times(1)).rollbackMIRs(rollbackEvent.getRollbackTo().getSlot());
    }
}
