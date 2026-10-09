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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How the parser turns bytes into text, and how large a {@code logo} or {@code image} may be.
 * <ul>
 *   <li>Text that is valid UTF-8 is stored as text; anything else is stored as hex, for every text field,
 *       not decoded with replacement characters (which loses the bytes).</li>
 *   <li>A {@code logo} or {@code image} given as a list of chunks is joined as bytes, then decoded once.</li>
 *   <li>A {@code logo} or {@code image} over {@value Cip68DatumParser#URI_MAX_BYTES} bytes is dropped with a
 *       warning, and the rest of the datum is kept.</li>
 * </ul>
 */
class Cip68DatumParserTextTest {

    /** Not valid UTF-8: 0xff never appears in UTF-8, and 0xc3 needs a continuation byte. */
    private static final byte[] NOT_UTF8 = {(byte) 0xff, 0x00, (byte) 0xc3};
    private static final String NOT_UTF8_HEX = "ff00c3";

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

    // ---------- text or hex ----------

    @Test
    void storesInvalidUtf8AsHexInEveryTextField() throws Exception {
        MapPlutusData properties = new MapPlutusData();
        for (String key : List.of("name", "description", "ticker", "url", "mediaType")) {
            properties.put(BytesPlutusData.of(key), BytesPlutusData.of(NOT_UTF8));
        }

        ParsedCip68Datum parsed = parse(properties).orElseThrow();

        assertThat(parsed.name()).isEqualTo(NOT_UTF8_HEX);
        assertThat(parsed.description()).isEqualTo(NOT_UTF8_HEX);
        assertThat(parsed.ticker()).isEqualTo(NOT_UTF8_HEX);
        assertThat(parsed.url()).isEqualTo(NOT_UTF8_HEX);
        assertThat(parsed.mediaType()).isEqualTo(NOT_UTF8_HEX);
    }

    @Test
    void storesInvalidUtf8AsHexForLogoAndImage() throws Exception {
        MapPlutusData properties = props("name", "n");
        properties.put(BytesPlutusData.of("logo"), BytesPlutusData.of(NOT_UTF8));
        properties.put(BytesPlutusData.of("image"), BytesPlutusData.of(NOT_UTF8));

        ParsedCip68Datum parsed = parse(properties).orElseThrow();

        assertThat(parsed.logo()).isEqualTo(NOT_UTF8_HEX);
        assertThat(parsed.image()).isEqualTo(NOT_UTF8_HEX);
    }

    @Test
    void keepsValidUtf8AsTextAndStripsNullCharacters() throws Exception {
        ParsedCip68Datum parsed = parse(props("name", "Mālama €", "description", "a\0b")).orElseThrow();

        assertThat(parsed.name()).isEqualTo("Mālama €");
        assertThat(parsed.description()).isEqualTo("ab");
    }

    // ---------- chunks ----------

    @Test
    void joinsChunksAsBytesSoACharacterSplitAcrossChunksSurvives() throws Exception {
        // "€" is three bytes (e2 82 ac); cut it between the chunks
        byte[] euro = "€".getBytes(StandardCharsets.UTF_8);
        MapPlutusData properties = props("name", "n");
        properties.put(BytesPlutusData.of("logo"), ListPlutusData.of(
                BytesPlutusData.of("ipfs://a".getBytes(StandardCharsets.UTF_8)),
                BytesPlutusData.of(Arrays.copyOfRange(euro, 0, 2)),
                BytesPlutusData.of(Arrays.copyOfRange(euro, 2, 3)),
                BytesPlutusData.of("b".getBytes(StandardCharsets.UTF_8))));

        assertThat(parse(properties).orElseThrow().logo()).isEqualTo("ipfs://a€b");
    }

    @Test
    void ignoresListElementsThatAreNotByteStrings() throws Exception {
        MapPlutusData properties = props("name", "n");
        properties.put(BytesPlutusData.of("image"), ListPlutusData.of(
                BytesPlutusData.of("ipfs://"), BigIntPlutusData.of(7), BytesPlutusData.of("Qm")));

        assertThat(parse(properties).orElseThrow().image()).isEqualTo("ipfs://Qm");
    }

    @Test
    void anEmptyByteStringImageIsKeptEmptyButAnEmptyListIsNothing() throws Exception {
        MapPlutusData empty = props("name", "n");
        empty.put(BytesPlutusData.of("image"), BytesPlutusData.of(new byte[0]));
        MapPlutusData noChunks = props("name", "n");
        noChunks.put(BytesPlutusData.of("image"), ListPlutusData.of());

        assertThat(parse(empty).orElseThrow().image()).isEmpty();
        assertThat(parse(noChunks).orElseThrow().image()).isNull();
    }

    // ---------- size cap ----------

    @Test
    void keepsALogoAndAnImageAtTheLimit() throws Exception {
        MapPlutusData properties = props("name", "n");
        properties.put(BytesPlutusData.of("logo"), chunks(Cip68DatumParser.URI_MAX_BYTES));
        properties.put(BytesPlutusData.of("image"), chunks(Cip68DatumParser.URI_MAX_BYTES));

        ParsedCip68Datum parsed = parse(properties).orElseThrow();

        assertThat(parsed.logo()).hasSize(Cip68DatumParser.URI_MAX_BYTES);
        assertThat(parsed.image()).hasSize(Cip68DatumParser.URI_MAX_BYTES);
        assertThat(warnings()).isEmpty();
    }

    @Test
    void dropsALogoAndAnImageOverTheLimitButKeepsTheDatum() throws Exception {
        MapPlutusData properties = props("name", "Big", "description", "has a huge logo");
        properties.put(BytesPlutusData.of("logo"), chunks(Cip68DatumParser.URI_MAX_BYTES + 1));
        properties.put(BytesPlutusData.of("image"), chunks(Cip68DatumParser.URI_MAX_BYTES + 1));

        ParsedCip68Datum parsed = parse(properties).orElseThrow();

        assertThat(parsed.logo()).isNull();
        assertThat(parsed.image()).isNull();
        assertThat(parsed.name()).isEqualTo("Big");
        assertThat(parsed.description()).isEqualTo("has a huge logo");
        assertThat(warnings()).hasSize(2).allSatisfy(w -> assertThat(w).contains("Ignoring CIP-68").contains("max 65536"));
    }

    @Test
    void appliesTheLimitToASingleByteStringToo() throws Exception {
        // not a list of chunks: one byte string over the limit (the CBOR encoder splits it on the wire)
        byte[] big = new byte[Cip68DatumParser.URI_MAX_BYTES + 1];
        Arrays.fill(big, (byte) 'a');
        MapPlutusData properties = props("name", "n");
        properties.put(BytesPlutusData.of("image"), BytesPlutusData.of(big));

        ParsedCip68Datum parsed = parse(properties).orElseThrow();

        assertThat(parsed.image()).isNull();
        assertThat(parsed.name()).isEqualTo("n");
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("'image'").contains("65537 bytes"));
    }

    // ---------- helpers ----------

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static MapPlutusData props(String... keyValues) {
        MapPlutusData map = new MapPlutusData();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(BytesPlutusData.of(keyValues[i]), BytesPlutusData.of(keyValues[i + 1]));
        }
        return map;
    }

    /** {@code size} bytes of ASCII as a list of 64-byte chunks, the way a long URI is stored on-chain. */
    private static ListPlutusData chunks(int size) {
        List<PlutusData> list = new ArrayList<>();
        int left = size;
        while (left > 0) {
            int n = Math.min(64, left);
            byte[] chunk = new byte[n];
            Arrays.fill(chunk, (byte) 'a');
            list.add(BytesPlutusData.of(chunk));
            left -= n;
        }
        return ListPlutusData.of(list.toArray(new PlutusData[0]));
    }

    private Optional<ParsedCip68Datum> parse(MapPlutusData properties) throws Exception {
        String hex = HexUtil.encodeHexString(CborSerializationUtil.serialize(
                ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
        return parser.parse(hex);
    }
}
