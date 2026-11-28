package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.domain.DatumEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collection;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getDatumHash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CollateralReturnIndexingTest {
    private final ScriptStorage scripts = mock(ScriptStorage.class);
    private final DatumStorage datums = mock(DatumStorage.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ScriptRefProcessor scriptProcessor = new ScriptRefProcessor(scripts);
    private final OutputDatumProcessor datumProcessor = new OutputDatumProcessor(datums, publisher);

    @Test
    void invalidTransactionIndexesOnlyCollateralReturn() {
        var event = event(true, List.of(output("11", "01"), output("33", "03")), output("22", "02"));
        process(event);
        assertSaved("22", "02", 2);
    }

    @Test
    void collateralReturnWithoutOrdinaryOutputsHasIndexZero() {
        process(event(true, List.of(), output("22", "02")));
        assertSaved("22", "02", 0);
    }

    @Test
    void validTransactionIndexesOnlyOrdinaryOutput() {
        process(event(false, List.of(output("11", "01")), output("22", "02")));
        assertSaved("11", "01", 0);
    }

    @Test
    void invalidWithoutReturnDoesNotIndexOrdinaryOutputs() {
        process(event(true, List.of(output("11", "01")), null));
        verifyNoInteractions(scripts, datums, publisher);
    }

    @SuppressWarnings("unchecked")
    private void assertSaved(String key, String datum, int index) {
        ArgumentCaptor<List<Script>> scriptCaptor = ArgumentCaptor.forClass(List.class);
        verify(scripts).saveScripts(scriptCaptor.capture());
        assertThat(scriptCaptor.getValue()).hasSize(1);
        assertThat(scriptCaptor.getValue().getFirst().getContent()).contains(key.repeat(28));

        ArgumentCaptor<Collection<Datum>> datumCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(datums).saveAll(datumCaptor.capture());
        assertThat(datumCaptor.getValue()).extracting(Datum::getDatum).containsExactly(datum);
        assertThat(datumCaptor.getValue()).extracting(Datum::getHash).containsExactly(getDatumHash(datum));

        ArgumentCaptor<DatumEvent> eventCaptor = ArgumentCaptor.forClass(DatumEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOutputDatums()).hasSize(1);
        assertThat(eventCaptor.getValue().getOutputDatums().getFirst().getOutputIndex()).isEqualTo(index);
        assertThat(eventCaptor.getValue().getWitnessDatums()).isEmpty();
    }

    private void process(TransactionEvent event) {
        scriptProcessor.processScriptRefInUtxo(event);
        datumProcessor.handleOutputDatumInTransaction(event);
    }

    private TransactionOutput output(String key, String datum) {
        return TransactionOutput.builder().address("addr_test").inlineDatum(datum)
                .scriptRef("82008200581c" + key.repeat(28)).build();
    }

    private TransactionEvent event(boolean invalid, List<TransactionOutput> outputs, TransactionOutput collateralReturn) {
        var transaction = mock(Transaction.class, RETURNS_DEEP_STUBS);
        when(transaction.isInvalid()).thenReturn(invalid);
        when(transaction.getTxHash()).thenReturn("aa".repeat(32));
        when(transaction.getBody()).thenReturn(TransactionBody.builder().outputs(outputs).collateralReturn(collateralReturn).build());
        when(transaction.getWitnesses().getDatums()).thenReturn(List.of());
        return TransactionEvent.builder().metadata(EventMetadata.builder().slot(100).block(10).build())
                .transactions(List.of(transaction)).build();
    }
}
