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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CIP-68 allows several user tokens for one reference NFT, for example a 222 and a 333 with the same policy and base
 * name. The row is stored with the first label (222, then 333, then 444), and the datum has to satisfy the
 * requirements of every label the transaction pairs it with: a token that is also a fungible token needs a
 * description, one that is also an NFT needs an image. A datum that fails one of them is not indexed, and the warning
 * says which label it fails.
 */
class Cip68MultiLabelTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String BASE = "4e4654";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    private static final String NFT = Cip68Constants.NFT_TOKEN_PREFIX;
    private static final String FT = Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
    private static final String RFT = Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX;

    private Cip68MetadataRepository repository;
    private Cip68Processor processor;
    private MeterRegistry registry;
    private ListAppender<ILoggingEvent> logs;
    private Logger processorLogger;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68MetadataRepository.class);
        registry = new SimpleMeterRegistry();
        Cip68Metrics metrics = new Cip68Metrics(registry);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(metrics), repository, metrics);
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
    void indexesADatumThatSatisfiesEveryLabelWithTheFirstOne() {
        Optional<Cip68Metadata> saved = index(datum("name", "LP", "description", "A pool token", "image", "ipfs://Qm"), NFT, FT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(warnings()).isEmpty();
        assertThat(infos()).singleElement().satisfies(i -> assertThat(i)
                .contains(POLICY).contains("[222, 333]").contains("stored with label 222"));
        assertThat(multiLabel("222+333", "indexed")).isEqualTo(1);
    }

    @Test
    void dropsTheDatumWhenItFailsTheSecondLabel() {
        // valid as a 222 (name and image), but a 333 needs a description
        Optional<Cip68Metadata> saved = index(datum("name", "LP", "image", "ipfs://Qm"), NFT, FT);

        assertThat(saved).isEmpty();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("Skipping CIP-68 datum").contains(POLICY).contains("(labels [222, 333])")
                .contains("not valid as label 333").contains("no description"));
        assertThat(skipped(333, "no_description")).isEqualTo(1);
        assertThat(multiLabel("222+333", "skipped")).isEqualTo(1);
    }

    @Test
    void dropsTheDatumWhenItFailsTheFirstLabel() {
        // valid as a 333 (name and description), but a 222 needs an image
        Optional<Cip68Metadata> saved = index(datum("name", "LP", "description", "A pool token"), NFT, FT);

        assertThat(saved).isEmpty();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("not valid as label 222").contains("no image"));
        assertThat(skipped(222, "no_image")).isEqualTo(1);
        assertThat(multiLabel("222+333", "skipped")).isEqualTo(1);
    }

    @Test
    void aBadFungibleLogoDoesNotRejectATokenThatIsAlsoAnNft() {
        Optional<Cip68Metadata> saved = index(
                datum("name", "LP", "description", "d", "image", "ipfs://Qm", "logo", "iagon://logo"), NFT, FT);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
            assertThat(row.getLogo()).isNull();
        });
        assertThat(multiLabel("222+333", "indexed")).isEqualTo(1);
    }

    @Test
    void handlesThreeLabels() {
        Optional<Cip68Metadata> saved = index(datum("name", "X", "description", "d", "image", "ipfs://Qm"), NFT, FT, RFT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(multiLabel("222+333+444", "indexed")).isEqualTo(1);
    }

    @Test
    void aFungibleAndRichFungiblePairNeedsDescriptionAndImage() {
        // 333 + 444: stored as 333, and it must also have the 444 image
        assertThat(index(datum("name", "X", "description", "d"), FT, RFT)).isEmpty();
        assertThat(skipped(444, "no_image")).isEqualTo(1);
    }

    @Test
    void aSingleLabelIsUnchanged() {
        Optional<Cip68Metadata> saved = index(datum("name", "N", "image", "ipfs://Qm"), NFT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(infos()).isEmpty();
        assertThat(registry.find(Cip68Metrics.MULTI_LABEL).counters()).isEmpty();
    }

    @Test
    void aSingleLabelWarningKeepsTheOldFormat() {
        index(datum("name", "N"), NFT);

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("(label 222)").doesNotContain("labels"));
    }

    // ---------- helpers ----------

    private double skipped(int label, String reason) {
        var c = registry.find(Cip68Metrics.SKIPPED).tag("label", String.valueOf(label)).tag("reason", reason).counter();
        return c == null ? 0 : c.count();
    }

    private double multiLabel(String labels, String outcome) {
        var c = registry.find(Cip68Metrics.MULTI_LABEL).tag("labels", labels).tag("outcome", outcome).counter();
        return c == null ? 0 : c.count();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<String> infos() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.INFO).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** Indexes one transaction: the reference NFT with the datum, plus one user token per given prefix. */
    private Optional<Cip68Metadata> index(String datum, String... userTokenPrefixes) {
        List<AddressUtxo> outputs = new ArrayList<>();
        outputs.add(AddressUtxo.builder().txHash(TX_HASH).txIndex(0).inlineDatum(datum)
                .amounts(List.of(amount(POLICY + Cip68Constants.REFERENCE_TOKEN_PREFIX + BASE))).build());
        for (int i = 0; i < userTokenPrefixes.length; i++) {
            outputs.add(AddressUtxo.builder().txHash(TX_HASH).txIndex(i + 1)
                    .amounts(List.of(amount(POLICY + userTokenPrefixes[i] + BASE))).build());
        }
        processor.processTransaction(AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(outputs).build()))
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
        try {
            verify(repository).saveAll(captor.capture());
        } catch (AssertionError notSaved) {
            verify(repository, never()).saveAll(any());
            return Optional.empty();
        }
        List<Cip68Metadata> rows = new ArrayList<>();
        captor.getValue().forEach(rows::add);
        assertThat(rows).hasSize(1);
        return Optional.of(rows.getFirst());
    }

    private static String datum(String... keyValues) {
        MapPlutusData properties = new MapPlutusData();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(BytesPlutusData.of(keyValues[i]), BytesPlutusData.of(keyValues[i + 1]));
        }
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Amt amount(String unit) {
        return Amt.builder().unit(unit).quantity(BigInteger.ONE).build();
    }
}
