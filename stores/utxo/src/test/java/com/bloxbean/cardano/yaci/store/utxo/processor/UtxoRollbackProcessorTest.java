package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtxoRollbackProcessorTest {

    @Mock
    private UtxoStorage utxoStorage;

    @Mock
    private InvalidTransactionStorage invalidTransactionStorage;

    @InjectMocks
    private UtxoRollbackProcessor utxoRollbackProcessor;

    @BeforeEach
    void setup() {

    }

    @Test
    void handleRollbackEvent() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .currentPoint(new Point(78650212, "07de1aad598dd499d44be8bd8c28d5cadf4f47fbfb6b2144ccfb376c996ffcec"))
                .rollbackTo(new Point(78650198, "ff86fbda2fc051e9337c9bcd7d58d82948000cb7ed5658f306046ad154f8389e"))
                .build();

        utxoRollbackProcessor.handleRollbackEvent(rollbackEvent);

        Mockito.verify(utxoStorage, Mockito.times(1)).deleteUnspentBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        Mockito.verify(invalidTransactionStorage, Mockito.times(1)).deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
    }
}
