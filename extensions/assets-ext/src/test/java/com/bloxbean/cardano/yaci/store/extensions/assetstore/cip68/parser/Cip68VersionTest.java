package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CIP-68 defines datum versions 1 to 4. A datum with another version is not indexed: one warning names the token and
 * the version, and the datum is counted as skipped with the reason {@code invalid_version}. For versions 1 to 4 the
 * layout still comes from the structure, as the CIP's retrieval steps say: a "721" key means nested metadata. The two
 * real datums are the only ones on mainnet that were not version 1, 2 or 3, and both are plain flat metadata maps.
 */
class Cip68VersionTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final AssetType REF = new AssetType(POLICY, "000643b0" + HexUtil.encodeHexString("Token".getBytes()));

    /** Mainnet: HOSKY 10K NFT 0002, first datum, version 100 (replaced by a version 1 datum, then burned). */
    private static final String HOSKY_VERSION_100_POLICY = "df9337b73a041b1c45015e4b08ee1fed9a8e2a5b2a25d73c6fc2a35b";
    private static final String HOSKY_VERSION_100_ASSET = "000643b0484f534b592054656e4b2030303032";
    private static final String HOSKY_VERSION_100_DATUM =
            "d8799fa4446e616d6552484f534b592031304b204e4654203030303245696d6167655835697066733a2f2f516d665276"
                    + "58375a41334673436a6f58575966425847534846573641525961617578364754335774714c455836674b646573637269"
                    + "7074696f6e5825546869732069732061207265666572656e636520746f6b656e20666f72204349502d36382e46747261"
                    + "697473a14474797065497265666572656e6365186480ff";

    /** Mainnet: Greenland Reserve Coin, current datum, version 0 (a live fungible token). */
    private static final String GNRC_VERSION_0_POLICY = "67cee89d59ab5354ee22c8af0638224126aecc6210f9372a61f13a64";
    private static final String GNRC_VERSION_0_ASSET = "000643b0474e5243";
    private static final String GNRC_VERSION_0_DATUM =
            "d8799fa6446e616d6556477265656e6c616e64205265736572766520436f696e4b6465736372697074696f6e583c4173"
                    + "736574206261636b656420746f6b656e207365637572656420627920477265656e6c616e642072756269657320616e64"
                    + "20736170706869726573467469636b657244474e52434375726c582368747470733a2f2f7777772e7468652d6d696e74"
                    + "2e636f6d2f636f6d706c69616e6365446c6f676f4048646563696d616c730600d866821a951b3c2b9f81581cc0bb241d"
                    + "37ffbdfdbb07d3d34bff54671c00935128da06966bc033810103ffff";

    private Cip68DatumParser parser;
    private MeterRegistry registry;
    private ListAppender<ILoggingEvent> logs;
    private Logger parserLogger;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        parser = new Cip68DatumParser(new Cip68Metrics(registry));
        parserLogger = (Logger) LoggerFactory.getLogger(Cip68DatumParser.class);
        logs = new ListAppender<>();
        logs.start();
        parserLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        parserLogger.detachAppender(logs);
    }

    @Test
    void acceptsEveryVersionTheCipDefines() throws Exception {
        for (long version = 1; version <= 4; version++) {
            Optional<ParsedCip68Datum> parsed = parser.parse(datum(version), REF);

            assertThat(parsed).as("version %d", version).isPresent();
            assertThat(parsed.get().version()).isEqualTo(version);
        }
        assertThat(warnings()).isEmpty();
    }

    @Test
    void rejectsVersionsTheCipDoesNotDefineWithOneWarningEach() throws Exception {
        for (long version : new long[]{0, 5, 100, -1, Long.MAX_VALUE}) {
            logs.list.clear();

            assertThat(parser.parse(datum(version), REF)).as("version %d", version).isEmpty();

            assertThat(warnings()).as("version %d", version).singleElement().satisfies(w -> assertThat(w)
                    .contains("Skipping CIP-68 datum").contains("version " + version).contains(POLICY).contains(REF.assetName())
                    .contains("is not one CIP-68 defines (1 to 4)"));
        }
        assertThat(invalidVersions()).isEqualTo(5);
    }

    @Test
    void namesNoTokenWhenThereIsNoReferenceNft() throws Exception {
        assertThat(parser.parse(datum(7))).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("version 7"));
    }

    @Test
    void doesNotCountTheVersionsItAccepts() throws Exception {
        for (long version = 1; version <= 4; version++) {
            parser.parse(datum(version), REF);
        }

        assertThat(invalidVersions()).isZero();
    }

    @Test
    void rejectsTheRealMainnetVersion100Datum() {
        // a test mint, replaced about 44,700 slots later by a valid version 1 datum and then burned
        AssetType ref = new AssetType(HOSKY_VERSION_100_POLICY, HOSKY_VERSION_100_ASSET);

        assertThat(parser.parse(HOSKY_VERSION_100_DATUM, ref)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("version 100").contains(HOSKY_VERSION_100_POLICY).contains(HOSKY_VERSION_100_ASSET));
    }

    @Test
    void rejectsTheRealMainnetVersion0Datum() {
        // Greenland Reserve Coin, a live fungible token; it is served from CIP-26 instead
        AssetType ref = new AssetType(GNRC_VERSION_0_POLICY, GNRC_VERSION_0_ASSET);

        assertThat(parser.parse(GNRC_VERSION_0_DATUM, ref)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("version 0").contains(GNRC_VERSION_0_POLICY));
    }

    @Test
    void readsANestedDatumAsNestedForTheVersionsItAccepts() throws Exception {
        for (long version : new long[]{1, 3, 4}) {
            String hex = nestedDatum(version);

            assertThat(parser.hasNestedMetadata(hex)).as("version %d", version).isTrue();
            assertThat(parser.parse(hex, REF).orElseThrow().name()).as("version %d", version).isEqualTo("Nested");
        }
    }

    @Test
    void rejectsANestedDatumOfAVersionTheCipDoesNotDefine() throws Exception {
        assertThat(parser.parse(nestedDatum(5), REF)).isEmpty();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("version 5"));
    }

    @Test
    void readsAVersion4DatumWithoutTheNestedKeyAsFlat() throws Exception {
        assertThat(parser.hasNestedMetadata(datum(4))).isFalse();
        assertThat(parser.parse(datum(4), REF).orElseThrow().name()).isEqualTo("Token");
        assertThat(warnings()).isEmpty();
    }

    private double invalidVersions() {
        var counter = registry.find(Cip68Metrics.SKIPPED).tag("label", "unknown").tag("reason", "invalid_version").counter();
        return counter == null ? 0 : counter.count();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** {"721": {policy: {asset name without label: metadata}}} for REF, with the given version. */
    private static String nestedDatum(long version) throws Exception {
        MapPlutusData metadata = new MapPlutusData();
        metadata.put(BytesPlutusData.of("name"), BytesPlutusData.of("Nested"));
        metadata.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
        MapPlutusData byAsset = new MapPlutusData();
        byAsset.put(BytesPlutusData.of(HexUtil.decodeHexString(REF.assetName().substring(8))), metadata);
        MapPlutusData byPolicy = new MapPlutusData();
        byPolicy.put(BytesPlutusData.of(HexUtil.decodeHexString(POLICY)), byAsset);
        MapPlutusData root = new MapPlutusData();
        root.put(BytesPlutusData.of("721"), byPolicy);
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                ConstrPlutusData.of(0, root, BigIntPlutusData.of(version)).serialize()));
    }

    /** A flat datum with a name and a description and the given version. */
    private static String datum(long version) throws Exception {
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
        properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                ConstrPlutusData.of(0, properties, BigIntPlutusData.of(BigInteger.valueOf(version))).serialize()));
    }
}
