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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code files} is optional in CIP-68, and each entry has to match {@code files_details}: a {@code mediaType} byte
 * string and a {@code src} that is a URI (https, ipfs, ar or data). When an entry does not, the {@code files} property is
 * left out as a whole, with a warning and a count, and the rest of the datum is kept.
 */
class Cip68FilesTest {

    private static final AssetType TOKEN = new AssetType("aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd", "000643b04e4654");

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
    void keepsFilesWhoseEntriesMatchTheDefinition() {
        ParsedCip68Datum parsed = parse(files(file("image/png", "ipfs://Qm1"), file("image/svg+xml", "https://x/y.svg")));

        assertThat(parsed.properties()).containsKey("files");
        assertThat(warnings()).isEmpty();
        assertThat(dropped()).isZero();
    }

    @Test
    void keepsAChunkedSrcThatIsAnAllowedUri() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("text/html"));
        file.put(BytesPlutusData.of("src"), ListPlutusData.of(BytesPlutusData.of("data:text/html;base64,"), BytesPlutusData.of("PGh0bWw+")));

        assertThat(parse(files(file)).properties()).containsKey("files");
        assertThat(warnings()).isEmpty();
    }

    @Test
    void dropsFilesButKeepsTheTokenWhenASrcIsABareIpfsHash() {
        ParsedCip68Datum parsed = parse(files(file("image/png", "ipfs://Qm1"), file("image/png", "QmZPgijpUjVjHbwJyTWW76nBrRzeMescSk4VUeL1ahbs8j")));

        assertDroppedAndTokenKept(parsed, "QmZPgijpUjVjHbwJyTWW76nBrRzeMescSk4VUeL1ahbs8j", "not a URI");
    }

    @Test
    void dropsFilesWhenASrcIsEmpty() {
        assertDroppedAndTokenKept(parse(files(file("image/png", ""))), "", "no usable src");
    }

    @Test
    void dropsFilesWhenAnEntryHasNoMediaType() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://Qm"));

        assertDroppedAndTokenKept(parse(files(file)), "", "has no mediaType");
    }

    @Test
    void dropsFilesWhenAnEntryHasNoSrc() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));

        assertDroppedAndTokenKept(parse(files(file)), "", "no usable src");
    }

    @Test
    void dropsFilesWhenAnEntryIsNotAMap() {
        assertDroppedAndTokenKept(parse(files(BigIntPlutusData.of(1))), "", "is not a map");
    }

    private void assertDroppedAndTokenKept(ParsedCip68Datum parsed, String value, String reason) {
        assertThat(parsed.name()).isEqualTo("N");
        assertThat(parsed.image()).isEqualTo("ipfs://img");
        assertThat(parsed.properties() == null || !parsed.properties().containsKey("files")).isTrue();
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("dropping property 'files' and keeping the rest").contains(value).contains(reason));
        assertThat(dropped()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private static MapPlutusData file(String mediaType, String src) {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of(mediaType));
        file.put(BytesPlutusData.of("src"), BytesPlutusData.of(src));
        return file;
    }

    private static MapPlutusData files(com.bloxbean.cardano.client.plutus.spec.PlutusData... entries) {
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of("N"));
        props.put(BytesPlutusData.of("image"), BytesPlutusData.of("ipfs://img"));
        props.put(BytesPlutusData.of("files"), ListPlutusData.of(entries));
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

    private double dropped() {
        var counter = registry.find(Cip68Metrics.DROPPED_PROPERTIES).tag("kind", Cip68Metrics.INVALID_FILES).counter();
        return counter == null ? 0 : counter.count();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }
}
