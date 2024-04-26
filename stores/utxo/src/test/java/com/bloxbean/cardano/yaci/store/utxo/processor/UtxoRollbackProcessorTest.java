package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtxoRollbackProcessorTest {

    @Mock
    private UtxoStorage utxoStorage;

    @InjectMocks
    private UtxoRollbackProcessor utxoRollbackProcessor;

    @BeforeEach
    void setup() {

    }

}
