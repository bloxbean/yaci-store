package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generic CIP-68 definition allows a metadata value to be a map, a list, an integer or a byte string; Plutus
 * data of any kind is allowed only in {@code extra}. A property whose value holds a constructor is therefore not
 * valid metadata. It is dropped with a warning that names the token and the property, and the rest of the datum,
 * and the token, are kept.
 */
class Cip68ConstructorPropertyTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String ASSET_NAME = "000643b04e4654";
    private static final AssetType TOKEN = new AssetType(POLICY, ASSET_NAME);

    private final Cip68DatumParser parser = new Cip68DatumParser();
    private ListAppender<ILoggingEvent> logs;
    private Logger parserLogger;

    @BeforeEach
    void setUp() {
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
    void dropsAConstructorPropertyKeepsTheRestAndWarns() {
        ParsedCip68Datum parsed = parse(properties(
                text("name", "Voucher"), text("description", "d"), text("image", "ipfs://Qm"),
                entry("owner", constr(0, BytesPlutusData.of(new byte[]{1, 2, 3}))),
                entry("tokens", BigIntPlutusData.of(5)),
                text("note", "kept")));

        assertThat(parsed.name()).isEqualTo("Voucher");
        assertThat(additional(parsed)).containsOnlyKeys("tokens", "note");
        assertThat(additional(parsed)).containsEntry("tokens", BigInteger.valueOf(5)).containsEntry("note", "kept");
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains(POLICY).contains(ASSET_NAME).contains("'owner'").contains("constructor"));
    }

    @Test
    void dropsThePropertyWhenTheConstructorIsInsideAList() {
        ParsedCip68Datum parsed = parse(properties(
                text("name", "N"), entry("holders", ListPlutusData.of(BigIntPlutusData.of(1), constr(1))), text("kept", "yes")));

        assertThat(additional(parsed)).containsOnlyKeys("kept");
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("'holders'"));
    }

    @Test
    void dropsThePropertyWhenTheConstructorIsInsideAMap() {
        MapPlutusData inner = new MapPlutusData();
        inner.put(BytesPlutusData.of("a"), BigIntPlutusData.of(1));
        inner.put(BytesPlutusData.of("b"), constr(0));

        ParsedCip68Datum parsed = parse(properties(text("name", "N"), entry("nested", inner), text("kept", "yes")));

        assertThat(additional(parsed)).containsOnlyKeys("kept");
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("'nested'"));
    }

    @Test
    void dropsOneWarningPerDroppedProperty() {
        ParsedCip68Datum parsed = parse(properties(
                text("name", "N"), entry("seed", constr(0)), entry("owner", constr(0)), text("kept", "yes")));

        assertThat(additional(parsed)).containsOnlyKeys("kept");
        assertThat(warnings()).hasSize(2);
    }

    @Test
    void dropsAConstructorPropertyOfAFileEntryAndKeepsTheFile() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://Qm"));
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));
        file.put(BytesPlutusData.of("extra"), constr(0));

        ParsedCip68Datum parsed = parse(properties(text("name", "N"), entry("files", ListPlutusData.of(file))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) parsed.properties().get("files");
        assertThat(files).singleElement().satisfies(f -> {
            assertThat(f).containsEntry("src", "ipfs://Qm").containsEntry("mediaType", "image/png");
            assertThat(f).doesNotContainKey("extra");
        });
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("files[].extra"));
    }

    @Test
    void keepsEverythingTheCipAllowsWithoutWarning() {
        MapPlutusData map = new MapPlutusData();
        map.put(BytesPlutusData.of("k"), BigIntPlutusData.of(1));

        ParsedCip68Datum parsed = parse(properties(
                text("name", "N"),
                entry("int", BigIntPlutusData.of(new BigInteger("123456789012345678901234567890"))),
                entry("list", ListPlutusData.of(BytesPlutusData.of("a"), BigIntPlutusData.of(2))),
                entry("map", map),
                text("bytes", "text")));

        assertThat(additional(parsed)).containsOnlyKeys("int", "list", "map", "bytes");
        assertThat(additional(parsed).get("int")).isEqualTo(new BigInteger("123456789012345678901234567890"));
        assertThat(warnings()).isEmpty();
    }

    @Test
    void aLongPropertyNameIsCutInTheWarning() {
        String key = "k".repeat(300);

        parse(properties(text("name", "N"), entry(key, constr(0))));

        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("k".repeat(60) + "...").doesNotContain("k".repeat(61)));
    }

    @Test
    void warnsWithoutATokenWhenThereIsNone() {
        // parse(datum) without the reference NFT, as the single-argument overload does
        String hex = datum(properties(text("name", "N"), entry("seed", constr(0))));
        ParsedCip68Datum parsed = parser.parse(hex).orElseThrow();

        assertThat(additional(parsed)).isNull();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("'seed'"));
    }

    // ---------- helpers ----------

    private ParsedCip68Datum parse(MapPlutusData properties) {
        return parser.parse(datum(properties), TOKEN).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> additional(ParsedCip68Datum parsed) {
        return parsed.properties() == null ? null : (Map<String, Object>) parsed.properties().get("additional_properties");
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static ConstrPlutusData constr(long alternative, PlutusData... fields) {
        ListPlutusData list = new ListPlutusData();
        for (PlutusData f : fields) {
            list.add(f);
        }
        return ConstrPlutusData.builder().alternative(alternative).data(list).build();
    }

    private static Map.Entry<String, PlutusData> text(String key, String value) {
        return Map.entry(key, BytesPlutusData.of(value));
    }

    private static Map.Entry<String, PlutusData> entry(String key, PlutusData value) {
        return Map.entry(key, value);
    }

    @SafeVarargs
    private static MapPlutusData properties(Map.Entry<String, PlutusData>... entries) {
        MapPlutusData map = new MapPlutusData();
        for (Map.Entry<String, PlutusData> e : entries) {
            map.put(BytesPlutusData.of(e.getKey()), e.getValue());
        }
        return map;
    }

    private static String datum(MapPlutusData properties) {
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
