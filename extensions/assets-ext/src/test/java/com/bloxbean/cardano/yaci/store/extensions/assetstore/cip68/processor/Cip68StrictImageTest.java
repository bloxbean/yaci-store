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
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CIP-68 requires {@code image} for the 222 NFT and the 444 RFT, as a URI with one of the schemes {@code https},
 * {@code ipfs}, {@code ar} or {@code data}. Parsing is strict about it: a token without an image, or with an
 * image of another scheme (live tokens use {@code iagon://}, and even free text), is not indexed and a warning
 * says which token was dropped and why. The 333 fungible token has no {@code image}; its {@code logo} is optional,
 * so one that is not a URI with one of the same schemes is left out with a warning and the token is still indexed.
 * <p>
 * Runs the real parser, token service and processor; only the repository is mocked.
 */
class Cip68StrictImageTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String BASE = "4e4654";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    /** Real mainnet reference-NFT datum of a "DID registration" sensor NFT: {@code mediaType} is image/svg+xml, but {@code image} is an empty byte string. */
    private static final String DID_SENSOR_POLICY = "2578304f310696dbc7892dea72cd5396cfd44a1e9a34cc7a2d410b43";
    private static final String DID_SENSOR_BASE = "323261663965313338366333366461616338636334313935";
    private static final String DID_SENSOR_DATUM =
            "d8799fbf4b6465736372697074696f6e583244494420726567697374726174696f6e20e28094206169722073656e736f"
                    + "7220727069352d72656d6f74652d30312d61697248646576696365496452727069352d72656d6f74652d30312d616972"
                    + "4364696458236469643a6d616c616d613a32326166396531333836633336646161633863633431393545696d61676540"
                    + "436c61745133322e3733313031343037373235353937436c6e67522d39362e3636383339393438373531313238496d65"
                    + "646961547970654d696d6167652f7376672b786d6c446e616d6558183232616639653133383663333664616163386363"
                    + "343139354870726f746f636f6c506d616c616d612d6f7261636c652d76314c7265676973746572656441745818323032"
                    + "362d30372d31325430313a30383a35372e3331365a4f73657276696365456e64706f696e74583c68747470733a2f2f61"
                    + "70692e64616777656c6c6465762e636f6d2f73656e736f72732f6c61746573742f727069352d72656d6f74652d30312d"
                    + "6169724f736574746c656d656e74436f756e744130467374617475734a72656769737465726564447479706543616972"
                    + "ff03d8799fa0581c000643b0323261663965313338366333366461616338636334313935ffff";

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

    @Test
    void dropsTheRealSensorNftWithAnEmptyImageAndWarns() {
        assertThat(index(DID_SENSOR_DATUM, DID_SENSOR_POLICY, DID_SENSOR_BASE, Cip68Constants.NFT_TOKEN_PREFIX)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("Skipping CIP-68 datum").contains(DID_SENSOR_POLICY).contains("000643b0" + DID_SENSOR_BASE)
                .contains("(label 222)").contains("no image"));
    }

    @Test
    void dropsAnNftWithoutAnImageKeyAndWarns() {
        assertThat(index(datum("name", "Plain NFT"), POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("no image").contains("(label 222)"));
    }

    @Test
    void dropsAnRftWithoutAnImageAndWarns() {
        assertThat(index(datum("name", "Rich token"), POLICY, BASE, Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("no image").contains("(label 444)"));
    }

    @Test
    void keepsAFungibleTokenWithoutAnImageWithoutWarning() {
        // 333 uses logo, not image
        Optional<Cip68Metadata> saved = index(datum("name", "Coin", "description", "A coin"), POLICY, BASE, Cip68Constants.FUNGIBLE_TOKEN_PREFIX);

        assertThat(saved).get().extracting(Cip68Metadata::getLabel).isEqualTo(Cip68Constants.LABEL_FT);
        assertThat(warnings()).isEmpty();
    }

    @Test
    void dropsAnIagonImageAndSaysWhy() {
        // live mainnet wine NFTs; wallets follow the CIP and cannot show iagon://, so Iagon would have to take it to the CIP
        String image = "iagon://675816de7e3fc1.26175981_ReserveChardonnayPG";

        assertThat(index(datum("name", "Wine", "image", image), POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("Skipping CIP-68 datum").contains(POLICY).contains("(label 222)")
                .contains(image).contains("not a URI with one of the schemes CIP-68 allows (https, ipfs, ar, data)"));
    }

    @Test
    void dropsFreeTextImageAndSaysWhy() {
        // seen on mainnet: an image that is not a URI at all
        assertThat(index(datum("name", "Odd", "image", "image IPFS here"), POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("image IPFS here").contains("not a URI"));
    }

    @Test
    void keepsImagesWithAnAllowedScheme() {
        for (String image : List.of("https://example.com/a.png", "ipfs://QmHash", "ar://txid", "data:image/svg+xml;base64,AAAA")) {
            org.mockito.Mockito.clearInvocations(repository);

            Optional<Cip68Metadata> saved = index(datum("name", "Ok", "image", image), POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX);

            assertThat(saved).as(image).get().extracting(Cip68Metadata::getImage).isEqualTo(image);
        }
        assertThat(warnings()).isEmpty();
    }

    @Test
    void keepsAChunkedIpfsImage() {
        // an image longer than 64 bytes is a list of chunks; the joined value is what is checked
        String image = "ipfs://" + "Q".repeat(80);
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Long"));
        properties.put(BytesPlutusData.of("image"), ListPlutusData.of(
                BytesPlutusData.of(image.substring(0, 50)), BytesPlutusData.of(image.substring(50))));

        Optional<Cip68Metadata> saved = index(datum(properties), POLICY, BASE, Cip68Constants.NFT_TOKEN_PREFIX);

        assertThat(saved).get().extracting(Cip68Metadata::getImage).isEqualTo(image);
    }

    @Test
    void keepsAFungibleTokenWhoseLogoHasAnotherSchemeButLeavesTheLogoOut() {
        assertLogoLeftOut("iagon://6911e6dd275fee62fb8917ba");
    }

    @Test
    void keepsAFungibleTokenWhoseLogoIsABareIpfsHashButLeavesTheLogoOut() {
        assertLogoLeftOut("QmZPgijpUjVjHbwJyTWW76nBrRzeMescSk4VUeL1ahbs8j");
    }

    private void assertLogoLeftOut(String logo) {
        Optional<Cip68Metadata> saved = index(datum("name", "Coin", "description", "A coin", "logo", logo),
                POLICY, BASE, Cip68Constants.FUNGIBLE_TOKEN_PREFIX);

        assertThat(saved).get().satisfies(row -> {
            assertThat(row.getLogo()).isNull();
            assertThat(row.getName()).isEqualTo("Coin");
            assertThat(row.getDescription()).isEqualTo("A coin");
            assertThat(row.getLabel()).isEqualTo(Cip68Constants.LABEL_FT);
        });
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("dropping the logo and keeping the rest").contains("its logo '" + logo + "'").contains("not a URI"));
    }

    @Test
    void keepsAFungibleTokenWhoseLogoHasAnAllowedScheme() {
        Optional<Cip68Metadata> saved = index(datum("name", "Coin", "description", "A coin", "logo", "https://example.com/l.png"),
                POLICY, BASE, Cip68Constants.FUNGIBLE_TOKEN_PREFIX);

        assertThat(saved).get().extracting(Cip68Metadata::getLogo).isEqualTo("https://example.com/l.png");
        assertThat(warnings()).isEmpty();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** Indexes one transaction (reference NFT with the datum, plus its paired user token) and returns the saved row, if any. */
    private Optional<Cip68Metadata> index(String datum, String policy, String base, String userTokenPrefix) {
        AddressUtxo refNft = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).inlineDatum(datum)
                .amounts(List.of(amount(policy + Cip68Constants.REFERENCE_TOKEN_PREFIX + base))).build();
        AddressUtxo userToken = AddressUtxo.builder().txHash(TX_HASH).txIndex(1)
                .amounts(List.of(amount(policy + userTokenPrefix + base))).build();
        processor.processTransaction(AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(refNft, userToken)).build()))
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

    /** A CIP-68 datum with the given text properties (key, value, key, value, ...) and version 1. */
    private static String datum(String... keyValues) {
        MapPlutusData properties = new MapPlutusData();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(BytesPlutusData.of(keyValues[i]), BytesPlutusData.of(keyValues[i + 1]));
        }
        return datum(properties);
    }

    private static String datum(MapPlutusData properties) {
        ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Amt amount(String unit) {
        return Amt.builder().unit(unit).quantity(BigInteger.ONE).build();
    }
}
