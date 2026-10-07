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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CIP-68 defines datum versions 1 to 4. A datum with another version is not indexed, for every label, and one
 * warning names the token. The two real datums are the only ones on mainnet that were not version 1, 2 or 3.
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
    private ListAppender<ILoggingEvent> logs;
    private Logger parserLogger;

    @BeforeEach
    void setUp() {
        parser = new Cip68DatumParser();
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
    void rejectsVersionsOutsideTheDefinedRangeWithOneWarningEach() throws Exception {
        for (long version : new long[]{0, 5, 100, -1, Long.MAX_VALUE}) {
            logs.list.clear();

            assertThat(parser.parse(datum(version), REF)).as("version %d", version).isEmpty();

            assertThat(warnings()).as("version %d", version).singleElement().satisfies(w -> assertThat(w)
                    .contains("unsupported version " + version).contains(POLICY).contains(REF.assetName())
                    .contains("defines 1 to 4"));
        }
    }

    @Test
    void namesNoTokenWhenThereIsNoReferenceNft() throws Exception {
        assertThat(parser.parse(datum(7))).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("unsupported version 7"));
    }

    @Test
    void rejectsTheRealMainnetVersion100Datum() {
        AssetType ref = new AssetType(HOSKY_VERSION_100_POLICY, HOSKY_VERSION_100_ASSET);

        assertThat(parser.parse(HOSKY_VERSION_100_DATUM, ref)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("unsupported version 100").contains(HOSKY_VERSION_100_POLICY).contains(HOSKY_VERSION_100_ASSET));
    }

    @Test
    void rejectsTheRealMainnetVersion0Datum() {
        AssetType ref = new AssetType(GNRC_VERSION_0_POLICY, GNRC_VERSION_0_ASSET);

        assertThat(parser.parse(GNRC_VERSION_0_DATUM, ref)).isEmpty();

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("unsupported version 0").contains(GNRC_VERSION_0_POLICY));
    }

    @Test
    void doesNotReadAnUnsupportedVersionAsNested() throws Exception {
        // a nested map under a version above 4 is not a nested datum: nothing is read from it
        MapPlutusData nested = new MapPlutusData();
        nested.put(BytesPlutusData.of("721"), new MapPlutusData());
        String hex = HexUtil.encodeHexString(CborSerializationUtil.serialize(
                ConstrPlutusData.of(0, nested, BigIntPlutusData.of(5)).serialize()));

        assertThat(parser.hasNestedMetadata(hex)).isFalse();
        assertThat(warnings()).isEmpty();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
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
