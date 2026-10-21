package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReason;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReport;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * An unexpected exception while indexing one output must not stop the sync (#1243, #1240): the output is skipped
 * with a warning and a {@code processing_failure} count, and the other outputs of the block are still indexed.
 * The failures are injected into a spy of the real parser, since the real one has no known way left to fail.
 */
class Cip68ProcessingFailureTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    private static final String BAD_DATUM = "bad0bad0";
    private static final String TOO_DEEP_DATUM = "dee0dee0";

    private MeterRegistry registry;
    private Cip68MetadataRepository repository;
    private Cip68DatumParser parser;
    private Cip68Processor processor;
    private ListAppender<ILoggingEvent> logs;
    private Logger processorLogger;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Cip68Metrics metrics = new Cip68Metrics(registry);
        repository = mock(Cip68MetadataRepository.class);
        parser = spy(new Cip68DatumParser(metrics));
        processor = new Cip68Processor(new Cip68TokenService(repository), parser, repository, metrics);
        processorLogger = (Logger) LoggerFactory.getLogger(Cip68Processor.class);
        logs = new ListAppender<>();
        logs.start();
        processorLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        processorLogger.detachAppender(logs);
    }

    @Test
    void anUnexpectedExceptionSkipsThatOutputAndTheOthersAreIndexed() {
        doThrow(new IllegalStateException("boom")).when(parser).parseDetailed(eq(BAD_DATUM), any(AssetType.class));

        // one block: a good token, the failing one, then a good token in the next transaction
        AddressUtxoEvent event = AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(
                        tx(output(goodDatum("First"), "01"), userToken("01"), output(BAD_DATUM, "02"), userToken("02")),
                        tx(output(goodDatum("Third"), "03"), userToken("03"))))
                .build();

        assertThatCode(() -> processor.processTransaction(event)).doesNotThrowAnyException();

        assertThat(saved()).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("First", "Third");
        assertThat(processingFailures()).isEqualTo(1);
        assertThat(indexed333()).isEqualTo(2);
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains(TX_HASH + "#").contains("indexing it failed unexpectedly")
                .contains("IllegalStateException").contains("boom").contains(BAD_DATUM));
    }

    @Test
    void oneReferenceNftFailingSkipsOnlyThatOneAndTheRestOfTheOutputIsIndexed() {
        // an output with the reference NFTs A, B and C, each read on its own (a nested datum): B throws, A and C are fine
        doReturn(true).when(parser).hasNestedMetadata(any());
        doThrow(new IllegalStateException("boom")).when(parser)
                .parseDetailed(any(), eq(AssetType.fromUnit(POLICY + Cip68Constants.REFERENCE_TOKEN_PREFIX + "02")));
        AddressUtxo shared = outputWithReferenceNfts(goodDatum("Fine"), "01", "02", "03");

        assertThatCode(() -> processor.processTransaction(event(tx(shared, userToken("01"), userToken("02"), userToken("03")))))
                .doesNotThrowAnyException();

        // A and C are stored (C used to be lost with B), B is skipped and counted once
        assertThat(saved()).extracting(Cip68Metadata::getAssetName)
                .containsExactlyInAnyOrder(Cip68Constants.REFERENCE_TOKEN_PREFIX + "01", Cip68Constants.REFERENCE_TOKEN_PREFIX + "03");
        assertThat(processingFailures()).isEqualTo(1);
        assertThat(indexed333()).isEqualTo(2);
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains(POLICY + "/" + Cip68Constants.REFERENCE_TOKEN_PREFIX + "02").contains("boom"));
    }

    @Test
    void onlyTheReferenceNftThatFailedIsRecordedInInvalidToken() {
        InvalidTokenRecorder recorder = mock(InvalidTokenRecorder.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), parser, repository, new Cip68Metrics(registry), recorder);
        doReturn(true).when(parser).hasNestedMetadata(any());
        doThrow(new IllegalStateException("boom")).when(parser)
                .parseDetailed(any(), eq(AssetType.fromUnit(POLICY + Cip68Constants.REFERENCE_TOKEN_PREFIX + "02")));

        processor.processTransaction(event(tx(outputWithReferenceNfts(goodDatum("Fine"), "01", "02", "03"),
                userToken("01"), userToken("02"), userToken("03"))));

        ArgumentCaptor<InvalidTokenReport> recorded = ArgumentCaptor.forClass(InvalidTokenReport.class);
        verify(recorder).record(recorded.capture());
        assertThat(recorded.getValue().reasonCode()).isEqualTo(InvalidTokenReason.PROCESSING_FAILURE);
        assertThat(recorded.getValue().assetName()).isEqualTo(Cip68Constants.REFERENCE_TOKEN_PREFIX + "02");
    }

    @Test
    void aFailureWhileParsingTheDatumSharedByReferenceNftsIsOneSkippedOutput() {
        // the shared flat datum is parsed when the output is first seen, before any token is read
        doThrow(new IllegalStateException("boom")).when(parser).parseDetailed(BAD_DATUM, null);

        AddressUtxo shared = outputWithReferenceNfts(BAD_DATUM, "01", "02", "03");
        AddressUtxoEvent event = event(tx(shared));

        assertThatCode(() -> processor.processTransaction(event)).doesNotThrowAnyException();

        verify(repository, never()).saveAll(any());
        assertThat(processingFailures()).isEqualTo(1);
    }

    @Test
    void propertiesThatCannotBeWrittenAsJsonSkipTheOutputBeforeAnythingIsCounted() {
        // Jackson refuses to write a document nested deeper than 1000 levels (#1240); the parser's own depth limit
        // is 100, so this is what would reach the flush if the parser ever let such a value through
        Map<String, Object> deep = new HashMap<>();
        Map<String, Object> level = deep;
        for (int i = 0; i < 1100; i++) {
            Map<String, Object> next = new HashMap<>();
            level.put("k", next);
            level = next;
        }
        ParsedCip68Datum parsed = new ParsedCip68Datum(null, "d", null, "Too deep", null, null, 1L, null, null, deep);
        doReturn(new com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParseOutcome.Parsed(parsed)).when(parser).parseDetailed(eq(TOO_DEEP_DATUM), any(AssetType.class));

        AddressUtxoEvent event = event(tx(
                output(TOO_DEEP_DATUM, "01"), userToken("01"),
                output(goodDatum("Fine"), "02"), userToken("02")));

        assertThatCode(() -> processor.processTransaction(event)).doesNotThrowAnyException();

        assertThat(saved()).extracting(Cip68Metadata::getName).containsExactly("Fine");
        assertThat(processingFailures()).isEqualTo(1);
        // not counted as indexed: it was never stored
        assertThat(indexed333()).isEqualTo(1);
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("indexing it failed unexpectedly").contains("cannot be written as JSON"));
    }

    @Test
    void aBlockWithoutFailuresCountsNothingAsAProcessingFailure() {
        processor.processTransaction(event(tx(output(goodDatum("Ok"), "01"), userToken("01"))));

        assertThat(saved()).extracting(Cip68Metadata::getName).containsExactly("Ok");
        assertThat(processingFailures()).isZero();
        assertThat(warnings()).isEmpty();
    }

    // ---------- helpers ----------

    private List<Cip68Metadata> saved() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cip68Metadata>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    private double processingFailures() {
        var c = registry.find(Cip68Metrics.SKIPPED)
                .tag("reason", Cip68Metrics.PROCESSING_FAILURE).counter();
        return c == null ? 0 : c.count();
    }

    private double indexed333() {
        var c = registry.find(Cip68Metrics.INDEXED).tag("label", "333").counter();
        return c == null ? 0 : c.count();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static AddressUtxoEvent event(TxInputOutput... txs) {
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(txs))
                .build();
    }

    private static TxInputOutput tx(AddressUtxo... outputs) {
        return TxInputOutput.builder().outputs(List.of(outputs)).build();
    }

    /** An output with the reference NFT of this base name (hex) and the given inline datum. */
    private static AddressUtxo output(String datum, String base) {
        return outputWithReferenceNfts(datum, base);
    }

    private static AddressUtxo outputWithReferenceNfts(String datum, String... bases) {
        List<Amt> amounts = new ArrayList<>();
        for (String base : bases) {
            amounts.add(Amt.builder().unit(POLICY + Cip68Constants.REFERENCE_TOKEN_PREFIX + base)
                    .quantity(BigInteger.ONE).build());
        }
        return AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0).inlineDatum(datum).amounts(amounts).build();
    }

    /** The fungible user token (333) paired with the reference NFT of the same base name. */
    private static AddressUtxo userToken(String base) {
        return AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(1)
                .amounts(List.of(Amt.builder().unit(POLICY + Cip68Constants.FUNGIBLE_TOKEN_PREFIX + base)
                        .quantity(BigInteger.ONE).build()))
                .build();
    }

    /** A valid flat datum of a fungible token: a name and a description. */
    private static String goodDatum(String name) {
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
        props.put(BytesPlutusData.of("description"), BytesPlutusData.of("d"));
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, props, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
