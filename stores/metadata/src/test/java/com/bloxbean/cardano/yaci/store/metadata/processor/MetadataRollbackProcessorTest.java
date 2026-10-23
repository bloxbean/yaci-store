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
    void handleRollbackEvent_keepsRowsAtRollbackPoint() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(115239200, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(115239162, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        metadataRollbackProcessor.handleRollbackEvent(rollbackEvent);

        Mockito.verify(txMetadataStorage, Mockito.times(1)).deleteBySlotGreaterThan(115239162);
    }

    @Test
    void handleRollbackEvent_rollbackPointReprocessed_alsoDeletesAtRollbackPoint() {
        //Restart with BlockFetch: the cursor block is processed again, and its rows have random ids
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(115239200, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(115239162, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .rollbackPointReprocessed(true)
                .build();

        metadataRollbackProcessor.handleRollbackEvent(rollbackEvent);

        Mockito.verify(txMetadataStorage, Mockito.times(1)).deleteBySlotGreaterThan(115239161);
    }
}
