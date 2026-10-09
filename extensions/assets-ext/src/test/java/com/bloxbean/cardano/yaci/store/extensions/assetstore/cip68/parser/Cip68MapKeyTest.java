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
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CIP-68 definition allows any metadata as a map key ({@code { * metadata => metadata }}), and JSON needs text
 * keys. A byte string key is text (or hex), an integer key is its decimal string, as Lucid and Blockfrost do. A list,
 * map or constructor key has no text form, so that entry is left out with a warning. When a byte string key and an integer key
 * read the same, the byte string key stays.
 */
class Cip68MapKeyTest {

    private static final AssetType TOKEN = new AssetType("aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd", "000643b04e4654");

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
    void anIntegerKeyOfAnAdditionalPropertyIsKeptAsItsDecimalString() {
        MapPlutusData props = name();
        props.put(BigIntPlutusData.of(7), BytesPlutusData.of("seven"));
        props.put(BigIntPlutusData.of(-3), BigIntPlutusData.of(30));

        assertThat(additional(parse(props))).containsEntry("7", "seven").containsEntry("-3", BigInteger.valueOf(30));
        assertThat(warnings()).isEmpty();
    }

    @Test
    void anIntegerKeyInsideANestedMapIsKept() {
        MapPlutusData inner = new MapPlutusData();
        inner.put(BigIntPlutusData.of(1), BytesPlutusData.of("one"));
        inner.put(BytesPlutusData.of("two"), BigIntPlutusData.of(2));
        MapPlutusData props = name();
        props.put(BytesPlutusData.of("attributes"), inner);

        assertThat(additional(parse(props)).get("attributes"))
                .isEqualTo(Map.of("1", "one", "two", BigInteger.TWO));
    }

    @Test
    void anIntegerKeyInAFileEntryIsKept() {
        MapPlutusData file = new MapPlutusData();
        file.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));
        file.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://Qm"));
        file.put(BigIntPlutusData.of(9), BytesPlutusData.of("nine"));
        MapPlutusData props = name();
        props.put(BytesPlutusData.of("files"), ListPlutusData.of(file));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) parse(props).properties().get("files");
        assertThat(files).singleElement().satisfies(f -> assertThat(f).containsEntry("src", "ipfs://Qm").containsEntry("9", "nine"));
    }

    @Test
    void aBigIntegerKeyIsKept() {
        MapPlutusData props = name();
        props.put(BigIntPlutusData.of(new BigInteger("123456789012345678901234567890")), BytesPlutusData.of("big"));

        assertThat(additional(parse(props))).containsEntry("123456789012345678901234567890", "big");
    }

    @Test
    void aListMapOrConstructorKeyIsLeftOutWithAWarning() {
        MapPlutusData props = name();
        props.put(ListPlutusData.of(BigIntPlutusData.of(1)), BytesPlutusData.of("a"));
        props.put(new MapPlutusData(), BytesPlutusData.of("b"));
        props.put(ConstrPlutusData.of(0), BytesPlutusData.of("c"));
        props.put(BytesPlutusData.of("kept"), BytesPlutusData.of("yes"));

        assertThat(additional(parse(props))).containsOnlyKeys("kept");
        assertThat(warnings()).hasSize(3);
        assertThat(warnings()).anySatisfy(w -> assertThat(w).contains(TOKEN.policyId()).contains("key is a list"));
        assertThat(warnings()).anySatisfy(w -> assertThat(w).contains("key is a map"));
        assertThat(warnings()).anySatisfy(w -> assertThat(w).contains("key is a constructor"));
    }

    @Test
    void whenAByteStringKeyAndAnIntegerKeyReadTheSameTheByteStringKeyStays() {
        MapPlutusData props = name();
        props.put(BytesPlutusData.of("1"), BytesPlutusData.of("from bytes"));
        props.put(BigIntPlutusData.of(1), BytesPlutusData.of("from integer"));

        assertThat(additional(parse(props))).containsEntry("1", "from bytes");
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("two keys read as '1'").contains("byte string key"));
    }

    @Test
    void byteStringKeysAreUnchanged() {
        MapPlutusData props = name();
        props.put(BytesPlutusData.of("trait"), BytesPlutusData.of("rare"));

        assertThat(additional(parse(props))).containsOnlyKeys("trait");
        assertThat(warnings()).isEmpty();
    }

    // ---------- helpers ----------

    private static MapPlutusData name() {
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of("N"));
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

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }
}
