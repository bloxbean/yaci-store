package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * What is dropped as an attack (ADR 0006 decision 3) can be found afterwards: each WARN names the token, and the transaction
 * when the processor knows it.
 */
class Cip68AttackLoggingTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String BASE = "4e4654";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    private Cip68Processor processor;
    private final List<ListAppender<ILoggingEvent>> appenders = new ArrayList<>();
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        Cip68Metrics metrics = Cip68Metrics.noop();
        Cip68MetadataRepository repository = mock(Cip68MetadataRepository.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(metrics), repository, metrics);
        logs = new ListAppender<>();
        logs.start();
        for (Class<?> c : new Class<?>[]{Cip68Processor.class, Cip68DatumParser.class}) {
            ((Logger) LoggerFactory.getLogger(c)).addAppender(logs);
        }
    }

    @AfterEach
    void tearDown() {
        for (Class<?> c : new Class<?>[]{Cip68Processor.class, Cip68DatumParser.class}) {
            ((Logger) LoggerFactory.getLogger(c)).detachAppender(logs);
        }
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void aPropertyNestedTooDeepIsLoggedWithTheTokenTheTransactionAndTheProperty() {
        PlutusData value = BigIntPlutusData.of(0);
        for (int i = 0; i <= Cip68DatumParser.MAX_PROPERTY_DEPTH; i++) {
            value = ListPlutusData.of(value);
        }
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of("N"));
        props.put(BytesPlutusData.of("attack"), value);

        processor.processTransaction(event(datum(props), "01"));

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains(TX_HASH).contains(POLICY).contains("000643b001").contains("'properties.attack'")
                .contains("nested deeper than 100 levels").contains("looks like an attack"));
    }

    @Test
    void aFlatDatumSharedByReferenceNftsIsLoggedOncePerOutput() {
        PlutusData value = BigIntPlutusData.of(0);
        for (int i = 0; i <= Cip68DatumParser.MAX_PROPERTY_DEPTH; i++) {
            value = ListPlutusData.of(value);
        }
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of("N"));
        props.put(BytesPlutusData.of("attack"), value);

        processor.processTransaction(event(datum(props), "01", "02", "03"));

        assertThat(warnings()).hasSize(1);
    }

    @Test
    void anUndecodableDatumIsLoggedWithTheTokenAndTheDatum() {
        processor.processTransaction(event("zz-not-hex", "01"));

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains(POLICY).contains("000643b001").contains("zz-not-hex").contains("unparseable"));
    }

    @Test
    void aDatumNestedTooDeeplyToDecodeIsLoggedWithTheTokenAndAShortenedDatum() {
        // 20,000 levels of one-element lists: deeper than the decoder's stack allows
        String hex = "d8799f" + "9f".repeat(20_000) + "01" + "ff".repeat(20_000) + "01ff";

        processor.processTransaction(event(hex, "01"));

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains(POLICY).contains("000643b001").contains("looks like an attack").contains("..."));
    }

    // ---------- helpers ----------

    private static String datum(MapPlutusData properties) {
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static AddressUtxoEvent event(String datum, String... baseSuffixes) {
        List<Amt> amounts = new ArrayList<>();
        for (String suffix : baseSuffixes) {
            amounts.add(Amt.builder().unit(POLICY + Cip68Constants.REFERENCE_TOKEN_PREFIX + suffix).quantity(BigInteger.ONE).build());
        }
        AddressUtxo output = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0).inlineDatum(datum).amounts(amounts).build();
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(output)).build()))
                .build();
    }
}
