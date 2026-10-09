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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The value of an additional property goes to a JSON column, and Jackson refuses to write a document nested deeper than
 * 1000 levels, which stopped the sync (#1240). The parser allows {@value Cip68DatumParser#MAX_PROPERTY_DEPTH} levels;
 * a property nested deeper is dropped with a warning and a count, and the rest of the datum is kept.
 */
class Cip68PropertyDepthTest {

    private static final AssetType TOKEN = new AssetType("aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd", "000643b04e4654");
    private static final int LIMIT = Cip68DatumParser.MAX_PROPERTY_DEPTH;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Cip68DatumParser parser = new Cip68DatumParser(new Cip68Metrics(registry));
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
    void keepsAValueNestedAtTheLimit() {
        ParsedCip68Datum parsed = parse(withProperty("foo", nested(LIMIT)));

        assertThat(additional(parsed)).containsKey("foo");
        assertThat(warnings()).isEmpty();
        assertThat(dropped()).isZero();
    }

    @Test
    void dropsAValueNestedOneLevelBeyondTheLimit() {
        ParsedCip68Datum parsed = parse(withProperty("foo", nested(LIMIT + 1)));

        assertThat(parsed.name()).isEqualTo("x");
        assertThat(parsed.description()).isEqualTo("d");
        assertThat(parsed.properties()).isNull();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("dropping property 'foo' and keeping the rest").contains("nested deeper than " + LIMIT + " levels"));
        assertThat(dropped()).isEqualTo(1);
    }

    @Test
    void dropsOnlyTheDeepPropertyAndKeepsTheOthers() {
        MapPlutusData props = withProperty("foo", nested(LIMIT + 1));
        props.put(BytesPlutusData.of("bar"), ListPlutusData.of(BigIntPlutusData.of(1)));

        assertThat(additional(parse(props))).containsOnlyKeys("bar");
    }

    @Test
    void countsDepthThroughMapsToo() {
        PlutusData value = BigIntPlutusData.of(0);
        for (int i = 0; i <= LIMIT; i++) {
            MapPlutusData map = new MapPlutusData();
            map.put(BytesPlutusData.of("k"), value);
            value = map;
        }

        assertThat(parse(withProperty("foo", value)).properties()).isNull();
        assertThat(dropped()).isEqualTo(1);
    }

    @Test
    void dropsAValueTooDeepInsideAFileEntryButKeepsTheEntry() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));
        file.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://Qm"));
        file.put(BytesPlutusData.of("deep"), nested(LIMIT + 1));
        MapPlutusData props = withProperty("bar", BigIntPlutusData.of(1));
        props.put(BytesPlutusData.of("files"), ListPlutusData.of(file));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) parse(props).properties().get("files");
        assertThat(files).singleElement().satisfies(f -> assertThat(f).containsOnlyKeys("mediaType", "src"));
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("files[].deep"));
        assertThat(dropped()).isEqualTo(1);
    }

    @Test
    void dropsAValueThatIsNestedFarBeyondTheLimitWithoutFailing() {
        // the depth of the datum in #1240, and a constructor at the bottom: the depth check runs before the constructor check
        String hex = "d8799f" + "a3"
                + "446e616d65" + "4178"
                + "4b6465736372697074696f6e" + "4164"
                + "43666f6f" + "81".repeat(1100) + "d87980"
                + "01" + "ff";

        ParsedCip68Datum parsed = parser.parse(hex, TOKEN).orElseThrow();

        assertThat(parsed.name()).isEqualTo("x");
        assertThat(parsed.properties()).isNull();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("nested deeper than"));
    }

    // ---------- helpers ----------

    /** A list nested {@code depth} levels: depth 1 is a list holding a number. */
    private static PlutusData nested(int depth) {
        PlutusData value = BigIntPlutusData.of(0);
        for (int i = 0; i < depth; i++) {
            value = ListPlutusData.of(value);
        }
        return value;
    }

    private static MapPlutusData withProperty(String key, PlutusData value) {
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of("x"));
        props.put(BytesPlutusData.of("description"), BytesPlutusData.of("d"));
        props.put(BytesPlutusData.of(key), value);
        return props;
    }

    private ParsedCip68Datum parse(MapPlutusData properties) {
        try {
            String hex = HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
            return parser.parse(hex, TOKEN).orElseThrow();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> additional(ParsedCip68Datum parsed) {
        return (Map<String, Object>) parsed.properties().get("additional_properties");
    }

    private double dropped() {
        var counter = registry.find(Cip68Metrics.DROPPED_PROPERTIES).tag("kind", Cip68Metrics.TOO_DEEP).counter();
        return counter == null ? 0 : counter.count();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }
}
