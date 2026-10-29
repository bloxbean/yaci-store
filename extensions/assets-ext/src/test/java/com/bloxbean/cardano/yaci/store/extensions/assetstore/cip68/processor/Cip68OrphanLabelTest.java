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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68Validator;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * A reference NFT whose user token is not in the same transaction cannot be paired, so its label is inferred from
 * the datum: {@code ticker} or {@code logo} mean a fungible token (333); {@code image}, {@code mediaType} or
 * {@code files} mean an NFT (222), or an RFT (444) when {@code decimals} is there too; otherwise 333. An exact
 * pairing always wins over this guess.
 * <p>
 * A datum with a missing required field is still indexed; served as a token of the label it was inferred as, it has a
 * MANDATORY error. The inferred label is only the one the row is stored with: each token is checked for its own label
 * when it is served.
 * <p>
 * Runs the real parser, token service and processor with the log captured; only the repository is mocked.
 */
class Cip68OrphanLabelTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String BASE = "4e4654";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    /** Real mainnet datum from bloxbean/yaci-store#1159: name, an empty image, a constructor, no description. */
    private static final String NFT_NO_DESCRIPTION =
            "d87982a3446e616d65464e465420233145696d616765404c636f6e747261637444617461d879860181581cdc9acfee35"
                    + "243d123e8f10bc58692a6bc5aa3135c7eafc2aac9daafcd87a80581c9abc17656a6d1c24688292777c18c1ce599845a5"
                    + "88f4d893c1884da2d87a80d87a8001";

    private Cip68MetadataRepository repository;
    private Cip68Processor processor;
    private ListAppender<ILoggingEvent> logs;
    private Logger processorLogger;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68MetadataRepository.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(), repository);
        processorLogger = (Logger) LoggerFactory.getLogger(Cip68Processor.class);
        logs = new ListAppender<>();
        logs.start();
        processorLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        processorLogger.detachAppender(logs);
    }

    // ---------- label inferred from the datum when nothing is paired ----------

    @Test
    void realWrappedSilverDatumIsAFungibleToken() {
        // real datum of a token whose own 333 token is not in this transaction (ticker, decimals, logo)
        Optional<Cip68Metadata> saved = index(Cip68LabelPairingTest.SILVER_DATUM, Cip68LabelPairingTest.SILVER_POLICY,
                Cip68LabelPairingTest.SILVER_BASE, null);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void nftWithoutDescriptionIsAnNftAndIsKept() {
        Optional<Cip68Metadata> saved = index(datum(text("name", "NFT #1"), text("image", "ipfs://Qm")), POLICY, BASE, null);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
            assertThat(row.getDescription()).isNull();
        });
    }

    @Test
    void realNftDatumWithAnEmptyImageIsIndexedWithAMandatoryImageError() {
        // the datum of #1159 has `image` as an empty byte string; CIP-68 requires an image for an NFT
        Optional<Cip68Metadata> saved = index(NFT_NO_DESCRIPTION, POLICY, BASE, null);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
            // the datum also has a constructor property, which is an error of its own
            assertThat(errors(row)).containsExactly("null:properties.contractData:OPTIONAL:plutus_constructor",
                    "222:image:MANDATORY:missing_value");
        });
        assertThat(warnings()).isEmpty();
    }

    @Test
    void imageAndDecimalsWithoutTickerOrLogoIsAnRft() {
        Optional<Cip68Metadata> saved = index(datum(text("name", "Rich"), text("image", "ipfs://Qm"), number("decimals", 2)),
                POLICY, BASE, null);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_RFT);
    }

    @Test
    void filesAloneMakeAnNftThatIsIndexedWithAMissingImageError() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));
        file.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://Qm"));
        Optional<Cip68Metadata> saved = index(datum(text("name", "Files"), entry("files", ListPlutusData.of(file))),
                POLICY, BASE, null);

        // inferred as 222, which CIP-68 requires an image for
        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
            assertThat(errors(row)).containsExactly("222:image:MANDATORY:missing_value");
        });
    }

    @Test
    void mediaTypeAloneMakesAnNftThatIsIndexedWithAMissingImageError() {
        Optional<Cip68Metadata> saved = index(datum(text("name", "Typed"), text("mediaType", "image/png")), POLICY, BASE, null);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
            assertThat(errors(row)).containsExactly("222:image:MANDATORY:missing_value");
        });
    }

    @Test
    void tickerWinsOverImageFields() {
        Optional<Cip68Metadata> saved = index(
                datum(text("name", "Mixed"), text("description", "d"), text("ticker", "MIX"), text("image", "ipfs://Qm")),
                POLICY, BASE, null);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void decimalsAloneDoesNotDecide() {
        // the fungible token and the RFT both define decimals: no other hint, so the historical default
        Optional<Cip68Metadata> saved = index(datum(text("name", "Dec"), text("description", "d"), number("decimals", 6)),
                POLICY, BASE, null);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void noHintFallsBackToFungibleToken() {
        Optional<Cip68Metadata> saved = index(datum(text("name", "Plain"), text("description", "d")), POLICY, BASE, null);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void aPairedUserTokenWinsOverTheDatumShape() {
        // a fungible-shaped datum, but its own 222 token is in the transaction: the pairing decides
        Optional<Cip68Metadata> saved = index(
                datum(text("name", "Paired"), text("description", "d"), text("ticker", "PRD"), text("image", "ipfs://Qm")),
                POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_NFT);
    }

    // ---------- what is wrong with a datum is reported when it is served ----------

    @Test
    void aFungibleTokenWithoutADescriptionIsIndexedWithAMandatoryError() {
        Optional<Cip68Metadata> saved = index(datum(text("name", "Coin"), text("ticker", "COIN")), POLICY, BASE, null);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_FT);
            assertThat(errors(row)).containsExactly("333:description:MANDATORY:missing_value");
            assertThat(row.getValidationErrors()).isEmpty();
            assertThat(Cip68Validator.validate(row, Cip68Constants.LABEL_FT).getFirst().error())
                    .isEqualTo("description field is missing a mandatory value");
        });
    }

    @Test
    void aDatumWithoutANameIsIndexedWithAMandatoryNameError() {
        Optional<Cip68Metadata> saved = index(datum(text("description", "no name here"), text("image", "ipfs://Qm")),
                POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getName()).isNull();
            assertThat(errors(row)).containsExactly("222:name:MANDATORY:missing_value");
        });
    }

    @Test
    void aDatumThatIsIndexedWithoutErrorsHasAnEmptyList() {
        Optional<Cip68Metadata> saved = index(datum(text("name", "Fine"), text("description", "d"), text("ticker", "FIN")), POLICY, BASE, null);

        assertThat(saved).get().satisfies(row -> assertThat(row.getValidationErrors()).isEmpty());
        assertThat(warnings()).isEmpty();
    }

    // ---------- helpers ----------

    /** The errors the row is served with as a token of the label it was stored with. */
    private static List<String> errors(Cip68Metadata row) {
        return Cip68Validator.validate(row, row.getLabel()).stream()
                .map(e -> e.subStandard() + ":" + e.fieldName() + ":" + e.fieldCategory() + ":" + e.code()).toList();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** Indexes one transaction (the reference NFT with the datum and, if given, its paired user token). */
    private Optional<Cip68Metadata> index(String datum, String policy, String base, String userTokenPrefix) {
        // an NFT's datum is stored once its reference NFT is (ADR 0002): these tests are about what is stored for it
        if (Cip68Constants.NFT_TOKEN_PREFIX.equals(userTokenPrefix)) {
            org.mockito.Mockito.when(repository.existsByPolicyIdAndAssetName(policy, Cip68Constants.REFERENCE_TOKEN_PREFIX + base)).thenReturn(true);
        }
        List<AddressUtxo> outputs = new ArrayList<>();
        outputs.add(AddressUtxo.builder().txHash(TX_HASH).txIndex(0).inlineDatum(datum)
                .amounts(List.of(amount(policy + Cip68Constants.REFERENCE_TOKEN_PREFIX + base))).build());
        if (userTokenPrefix != null) {
            outputs.add(AddressUtxo.builder().txHash(TX_HASH).txIndex(1)
                    .amounts(List.of(amount(policy + userTokenPrefix + base))).build());
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
            verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.any());
            return Optional.empty();
        }
        List<Cip68Metadata> rows = new ArrayList<>();
        captor.getValue().forEach(rows::add);
        assertThat(rows).hasSize(1);
        return Optional.of(rows.getFirst());
    }

    private static Map.Entry<String, PlutusData> text(String key, String value) {
        return entry(key, BytesPlutusData.of(value));
    }

    private static Map.Entry<String, PlutusData> number(String key, long value) {
        return entry(key, BigIntPlutusData.of(BigInteger.valueOf(value)));
    }

    private static Map.Entry<String, PlutusData> entry(String key, PlutusData value) {
        return Map.entry(key, value);
    }

    /** A CIP-68 datum with the given properties and version 1. */
    @SafeVarargs
    private static String datum(Map.Entry<String, PlutusData>... properties) {
        MapPlutusData map = new MapPlutusData();
        LinkedHashMap<String, PlutusData> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, PlutusData> p : properties) {
            ordered.put(p.getKey(), p.getValue());
        }
        ordered.forEach((k, v) -> map.put(BytesPlutusData.of(k), v));
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, map, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Amt amount(String unit) {
        return Amt.builder().unit(unit).quantity(BigInteger.ONE).build();
    }
}
