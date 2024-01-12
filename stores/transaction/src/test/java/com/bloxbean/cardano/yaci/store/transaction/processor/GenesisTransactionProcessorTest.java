package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GenesisTransactionProcessorTest {
    @Mock
    private TransactionStorage transactionStorage;

    @InjectMocks
    private GenesisTransactionProcessor genesisTransactionProcessor;

    @Captor
    private ArgumentCaptor<List<Txn>> txnListCaptor;

    @Test
    void givenGenesisBlockEvent_shouldHandleAndSaveTransactionData() {
        List<GenesisBalance> genesisBalances = List.of(new GenesisBalance(
                "FHnt4NL7yPXhCzCHVywZLqVsvwuG3HvwmjKXQJBrXh3h2aigv6uxkePbpzRNV8q",
                "8e0280beebc3d12626e87b182f4205d75e49981042f54081cd35f3a4a85630b0",
                BigInteger.ZERO
        ));
        GenesisBlockEvent genesisBlockEvent = GenesisBlockEvent.builder()
                .genesisBalances(genesisBalances)
                .blockHash("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937")
                .block(-1)
                .blockTime(1654041600)
                .slot(-1)
                .build();

        genesisTransactionProcessor.processGenesisUtxos(genesisBlockEvent);

        Mockito.verify(transactionStorage, Mockito.times(1)).saveAll(txnListCaptor.capture());
        List<Txn> txnList = txnListCaptor.getValue();

        assertThat(txnList).hasSize(1);

        assertThat(txnList.get(0).getBlockTime()).isEqualTo(1654041600);
        assertThat(txnList.get(0).getBlockNumber()).isEqualTo(-1);
        assertThat(txnList.get(0).getBlockHash()).isEqualTo("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937");
        assertThat(txnList.get(0).getSlot()).isEqualTo(-1);
        assertThat(txnList.get(0).getInvalid()).isFalse();

        assertThat(txnList.get(0).getTxHash()).isEqualTo("8e0280beebc3d12626e87b182f4205d75e49981042f54081cd35f3a4a85630b0");
        assertThat(txnList.get(0).getOutputs().get(0).getOutputIndex()).isEqualTo(0);
        assertThat(txnList.get(0).getOutputs().get(0).getTxHash()).isEqualTo("8e0280beebc3d12626e87b182f4205d75e49981042f54081cd35f3a4a85630b0");
        assertThat(txnList.get(0).getInputs()).hasSize(0);
    }

}
