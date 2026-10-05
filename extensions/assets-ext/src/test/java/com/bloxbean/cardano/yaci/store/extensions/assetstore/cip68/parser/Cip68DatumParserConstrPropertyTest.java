package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A metadata property whose value is a Plutus constructor has no JSON form. It used to make
 * {@code parseAdditionalProperties} throw a NullPointerException, so the whole datum was skipped
 * (#1159). Now only that property is left out and the rest of the metadata is kept.
 */
class Cip68DatumParserConstrPropertyTest {

    private static final String VECTORS = "/cip68/constr-valued-property-datums.txt";

    private Cip68DatumParser parser;

    @BeforeEach
    void setUp() {
        parser = new Cip68DatumParser();
    }

    @Test
    void shouldKeepMetadataWhenAdditionalPropertyIsAConstructor() throws Exception {
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("NFT #1"));
        properties.put(BytesPlutusData.of("rarity"), BytesPlutusData.of("legendary"));
        properties.put(BytesPlutusData.of("contractData"),
                ConstrPlutusData.of(0, BigIntPlutusData.of(1), BytesPlutusData.of("x")));

        Optional<ParsedCip68Datum> result = parser.parse(encode(properties, 1));

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("NFT #1");
        assertThat(additionalProperties(result.get()))
                .containsEntry("rarity", "legendary")
                .doesNotContainKey("contractData");
    }

    @Test
    void shouldNotAddAdditionalPropertiesWhenOnlyConstructorsAreLeft() throws Exception {
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("NFT #1"));
        properties.put(BytesPlutusData.of("owner"), ConstrPlutusData.of(0, BytesPlutusData.of("addr")));

        Optional<ParsedCip68Datum> result = parser.parse(encode(properties, 1));

        assertThat(result).isPresent();
        assertThat(result.get().properties()).isNull();
    }

    @Test
    void shouldKeepSiblingsOfAConstructorInsideAListOrMap() throws Exception {
        MapPlutusData nested = new MapPlutusData();
        nested.put(BytesPlutusData.of("keep"), BytesPlutusData.of("yes"));
        nested.put(BytesPlutusData.of("drop"), ConstrPlutusData.of(0, BigIntPlutusData.of(1)));
        ListPlutusData list = ListPlutusData.of(BytesPlutusData.of("a"), ConstrPlutusData.of(0), BytesPlutusData.of("b"));

        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("NFT #1"));
        properties.put(BytesPlutusData.of("attrs"), nested);
        properties.put(BytesPlutusData.of("tags"), list);

        Optional<ParsedCip68Datum> result = parser.parse(encode(properties, 1));

        assertThat(result).isPresent();
        assertThat(additionalProperties(result.get()))
                .containsEntry("attrs", Map.of("keep", "yes"))
                .containsEntry("tags", List.of("a", "b"));
    }

    @Test
    void shouldParseTheDatumFromIssue1159() {
        // First line of the vector file: name "NFT #1", empty image, contractData = constructor
        Optional<ParsedCip68Datum> result = parser.parse(vectors().getFirst());

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("NFT #1");
        assertThat(result.get().version()).isEqualTo(1L);
        assertThat(result.get().properties()).isNull();
    }

    @Test
    void shouldParseEveryMainnetDatumThatUsedToBeSkipped() {
        List<String> datums = vectors();

        List<String> skipped = datums.stream()
                .filter(datum -> parser.parse(datum).isEmpty())
                .toList();

        assertThat(datums).hasSizeGreaterThan(400);
        assertThat(skipped).as("datums the parser still drops").isEmpty();
    }

    private static String encode(MapPlutusData properties, int version) throws Exception {
        ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(BigInteger.valueOf(version)));
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> additionalProperties(ParsedCip68Datum parsed) {
        return (Map<String, Object>) parsed.properties().get("additional_properties");
    }

    private static List<String> vectors() {
        try (InputStream in = Cip68DatumParserConstrPropertyTest.class.getResourceAsStream(VECTORS)) {
            assertThat(in).as(VECTORS).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
