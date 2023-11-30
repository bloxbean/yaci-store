package com.bloxbean.cardano.yaci.store.metadata.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataRollbackProcessorTest {
    @Mock
    private TxMetadataStorage txMetadataStorage;

    @InjectMocks
    private MetadataRollbackProcessor metadataRollbackProcessor;

    @Test
    void testHandleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10620521, "8a37b9c53b0d1308dc30a90da83f08c565359955e8e4c1443040a9b4e0c8f29b"))
                .currentPoint(new Point(10623665, "8913504640f622541ea39b34e681cb3cca05c5e30115ecd7a6a2c018fbe2d4f9"))
                .build();

        metadataRollbackProcessor.handleRollbackEvent(rollbackEvent);

        Mockito.verify(txMetadataStorage, Mockito.times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }
}
