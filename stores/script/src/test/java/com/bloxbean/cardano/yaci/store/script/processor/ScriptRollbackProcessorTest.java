package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ScriptRollbackProcessorTest {

    @Mock
    private TxScriptStorage txScriptStorage;

    @InjectMocks
    private ScriptRollbackProcessor rollbackProcessor;

    @Test
    void handleRollbackEventOfEpochParamProcessor() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10684581, "d5fed4dca96c7efda3d1b0897c91c0eee8f8c6a18e5931dfbd56b6ec7a5951a1"))
                .currentPoint(new Point(10697261, "007f4409f067fc58c3c3cd1d44f7a9e418e5080e73173cc7db701cfb90c5f031"))
                .build();

        rollbackProcessor.handleRollbackEvent(rollbackEvent);

        Mockito.verify(txScriptStorage, Mockito.times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }
}
