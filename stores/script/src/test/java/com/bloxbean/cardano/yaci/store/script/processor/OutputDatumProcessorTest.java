package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutputDatumProcessorTest {

    @Mock
    private DatumStorage datumStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private OutputDatumProcessor outputDatumProcessor;

    @Captor
    private ArgumentCaptor<Collection<Datum>> datumCaptor;

    @Test
    void givenDeeplyNestedInlineDatum_shouldSkipItAndSaveOtherDatums() throws Throwable {
        String normalDatum = "d8799f182aff"; //Constr 0 [42]
        String deepDatum = "81".repeat(100_000) + "00";
        String normalDatumHash = PlutusData.deserialize(HexUtil.decodeHexString(normalDatum)).getDatumHash();

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .metadata(EventMetadata.builder().slot(100).build())
                .transactions(List.of(
                        Transaction.builder()
                                .txHash("0d66c70aa9efa6bc1a635191abef9a0cd386a42b3f43c3199f3b794986995865")
                                .body(TransactionBody.builder()
                                        .outputs(List.of(
                                                TransactionOutput.builder()
                                                        .address("addr_test1wpnlxv2xv9a9ucvnvzqakwepzl9ltx7jzgm53av2e9ncv4sysemm8")
                                                        .inlineDatum(normalDatum)
                                                        .build(),
                                                TransactionOutput.builder()
                                                        .address("addr_test1wpnlxv2xv9a9ucvnvzqakwepzl9ltx7jzgm53av2e9ncv4sysemm8")
                                                        .inlineDatum(deepDatum)
                                                        .build()
                                        ))
                                        .build())
                                .witnesses(Witnesses.builder().build())
                                .build()
                ))
                .build();

        //Small stack so that the deeply nested datum overflows deterministically
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                outputDatumProcessor.handleOutputDatumInTransaction(transactionEvent);
            } catch (Throwable t) {
                error.set(t);
            }
        }, "small-stack", 256 * 1024);
        thread.start();
        thread.join();

        assertThat(error.get()).isNull();

        Mockito.verify(datumStorage).saveAll(datumCaptor.capture());
        List<Datum> savedDatums = new ArrayList<>(datumCaptor.getValue());
        assertThat(savedDatums).hasSize(1);
        assertThat(savedDatums.get(0).getHash()).isEqualTo(normalDatumHash);
        assertThat(savedDatums.get(0).getDatum()).isEqualTo(normalDatum);

        Mockito.verify(publisher).publishEvent(Mockito.any(Object.class));
    }
}
