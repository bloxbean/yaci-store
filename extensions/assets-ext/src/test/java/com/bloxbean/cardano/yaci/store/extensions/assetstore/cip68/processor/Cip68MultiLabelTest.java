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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68Validator;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
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
 * name. The row is stored with the first label (222, then 333, then 444), with what is wrong with the datum itself;
 * each token is checked for its own label when it is served: a fungible token needs a description, an NFT or RFT an
 * image. A datum that fails one of them is still indexed, and each error names the label it was found for.
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

    private static List<String> errorsOf(Cip68Metadata row) {
        return format(row.getValidationErrors());
    }

    /** The errors the token of {@code label} is served with. */
    private static List<String> servedAs(Cip68Metadata row, int label) {
        return format(Cip68Validator.validate(row, label));
    }

    private static List<String> format(List<ValidationError> errors) {
        return errors.stream()
                .map(e -> e.subStandard() + ":" + e.fieldName() + ":" + e.fieldCategory() + ":" + e.code()).toList();
    }

    @Test
    void indexesADatumThatSatisfiesEveryLabelWithTheFirstOne() {
        Optional<Cip68Metadata> saved = index(datum("name", "LP", "description", "A pool token", "image", "ipfs://Qm"), NFT, FT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(saved.get().getValidationErrors()).isEmpty();
        assertThat(warnings()).isEmpty();
        assertThat(infos()).singleElement().satisfies(i -> assertThat(i)
                .contains(POLICY).contains("[222, 333]").contains("stored with label 222"));
    }

    @Test
    void indexesTheDatumAndReportsTheSecondLabelItFails() {
        // valid as a 222 (name and image), but a 333 needs a description
        Optional<Cip68Metadata> saved = index(datum("name", "LP", "image", "ipfs://Qm"), NFT, FT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(errorsOf(saved.get())).isEmpty();
        assertThat(servedAs(saved.get(), Cip68Constants.LABEL_NFT)).isEmpty();
        assertThat(servedAs(saved.get(), Cip68Constants.LABEL_FT)).containsExactly("333:description:MANDATORY:missing_value");
        assertThat(warnings()).isEmpty();
        assertThat(registry.find(Cip68Metrics.SKIPPED).counters()).isEmpty();
    }

    @Test
    void indexesTheDatumAndReportsTheFirstLabelItFails() {
        // valid as a 333 (name and description), but a 222 needs an image
        Optional<Cip68Metadata> saved = index(datum("name", "LP", "description", "A pool token"), NFT, FT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(servedAs(saved.get(), Cip68Constants.LABEL_NFT)).containsExactly("222:image:MANDATORY:missing_value");
        assertThat(servedAs(saved.get(), Cip68Constants.LABEL_FT)).isEmpty();
    }

    @Test
    void reportsEveryLabelThatFailsWithItsOwnSubStandard() {
        // a 222 + 333 + 444 datum with nothing but a name: 222 and 444 miss the image, 333 misses the description
        Optional<Cip68Metadata> saved = index(datum("name", "X"), NFT, FT, RFT);

        Cip68Metadata row = saved.orElseThrow();
        assertThat(servedAs(row, Cip68Constants.LABEL_NFT)).containsExactly("222:image:MANDATORY:missing_value");
        assertThat(servedAs(row, Cip68Constants.LABEL_FT)).containsExactly("333:description:MANDATORY:missing_value");
        assertThat(servedAs(row, Cip68Constants.LABEL_RFT)).containsExactly("444:image:MANDATORY:missing_value");
    }

    @Test
    void aBadFungibleLogoIsKeptAndReportedOnATokenThatIsAlsoAnNft() {
        Optional<Cip68Metadata> saved = index(
                datum("name", "LP", "description", "d", "image", "ipfs://Qm", "logo", "iagon://logo"), NFT, FT);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
            assertThat(row.getLogo()).isEqualTo("iagon://logo");
            // the logo does not depend on the label, so its error carries no sub_standard
            assertThat(errorsOf(row)).containsExactly("null:logo:OPTIONAL:invalid_uri_scheme");
        });
    }

    @Test
    void handlesThreeLabels() {
        Optional<Cip68Metadata> saved = index(datum("name", "X", "description", "d", "image", "ipfs://Qm"), NFT, FT, RFT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(saved.get().getValidationErrors()).isEmpty();
    }

    @Test
    void aFungibleAndRichFungiblePairReportsAMissingImageForTheRichFungibleLabel() {
        // 333 + 444: stored as 333, and the 444 image is missing
        Optional<Cip68Metadata> saved = index(datum("name", "X", "description", "d"), FT, RFT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_FT);
        assertThat(servedAs(saved.get(), Cip68Constants.LABEL_FT)).isEmpty();
        assertThat(servedAs(saved.get(), Cip68Constants.LABEL_RFT)).containsExactly("444:image:MANDATORY:missing_value");
    }

    @Test
    void aSingleLabelIsUnchanged() {
        Optional<Cip68Metadata> saved = index(datum("name", "N", "image", "ipfs://Qm"), NFT);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
        assertThat(infos()).isEmpty();
    }

    @Test
    void aSingleLabelWithAMissingFieldIsServedWithTheErrorForThatLabel() {
        Optional<Cip68Metadata> saved = index(datum("name", "N"), NFT);

        assertThat(servedAs(saved.orElseThrow(), Cip68Constants.LABEL_NFT)).containsExactly("222:image:MANDATORY:missing_value");
        assertThat(warnings()).isEmpty();
    }

    // ---------- helpers ----------

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private List<String> infos() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.INFO).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** Indexes one transaction: the reference NFT with the datum, plus one user token per given prefix. */
    private Optional<Cip68Metadata> index(String datum, String... userTokenPrefixes) {
        // an NFT's datum is stored once its reference NFT is (ADR 0002): these tests are about what is stored for it
        if (List.of(userTokenPrefixes).equals(List.of(NFT))) {
            org.mockito.Mockito.when(repository.existsByPolicyIdAndAssetName(POLICY, Cip68Constants.REFERENCE_TOKEN_PREFIX + BASE)).thenReturn(true);
        }
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
