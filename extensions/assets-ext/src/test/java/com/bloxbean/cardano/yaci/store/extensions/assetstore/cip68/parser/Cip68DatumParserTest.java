package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class Cip68DatumParserTest {

    private Cip68DatumParser parser;

    @BeforeEach
    void setUp() {
        parser = new Cip68DatumParser();
    }

    @Nested
    class ParseValidDatum {

        @Test
        void shouldParseAllFields() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("TestToken"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("A test token"));
            properties.put(BytesPlutusData.of("ticker"), BytesPlutusData.of("TT"));
            properties.put(BytesPlutusData.of("url"), BytesPlutusData.of("https://example.com"));
            properties.put(BytesPlutusData.of("decimals"), BigIntPlutusData.of(6));
            properties.put(BytesPlutusData.of("logo"), BytesPlutusData.of("iVBORw0KGgo="));

            ConstrPlutusData datum = ConstrPlutusData.of(0,
                    properties,
                    BigIntPlutusData.of(1));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            ParsedCip68Datum metadata = result.get();
            assertThat(metadata.name()).isEqualTo("TestToken");
            assertThat(metadata.description()).isEqualTo("A test token");
            assertThat(metadata.ticker()).isEqualTo("TT");
            assertThat(metadata.url()).isEqualTo("https://example.com");
            assertThat(metadata.decimals()).isEqualTo(6L);
            assertThat(metadata.logo()).isEqualTo("iVBORw0KGgo=");
            assertThat(metadata.version()).isEqualTo(1L);
        }

        @Test
        void shouldParseWithOnlyRequiredFields() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("MinToken"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Minimal"));

            ConstrPlutusData datum = ConstrPlutusData.of(0,
                    properties,
                    BigIntPlutusData.of(2));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            ParsedCip68Datum metadata = result.get();
            assertThat(metadata.name()).isEqualTo("MinToken");
            assertThat(metadata.description()).isEqualTo("Minimal");
            assertThat(metadata.ticker()).isNull();
            assertThat(metadata.url()).isNull();
            assertThat(metadata.decimals()).isNull();
            assertThat(metadata.logo()).isNull();
            assertThat(metadata.version()).isEqualTo(2L);
        }

        @Test
        void shouldStripNullCharactersFromStrings() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Test\0Token"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("A\0desc"));

            ConstrPlutusData datum = ConstrPlutusData.of(0,
                    properties,
                    BigIntPlutusData.of(1));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("TestToken");
            assertThat(result.get().description()).isEqualTo("Adesc");
        }

        @Test
        void shouldExtractNftFieldsImageAndMediaType() throws Exception {
            // CIP-68 NFT-shape datum (label 222) — exercises the image / mediaType code path.
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("KhonsuMoon #4651"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("A celestial collectible"));
            properties.put(BytesPlutusData.of("image"), BytesPlutusData.of("ipfs://QmMain"));
            properties.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().image()).isEqualTo("ipfs://QmMain");
            assertThat(result.get().mediaType()).isEqualTo("image/png");
        }

        @Test
        void shouldHandleChunkedImageUriAsListOfStrings() throws Exception {
            // CIP-25 inheritance: image may be split into a list of byte-string chunks
            // when the URI exceeds Cardano's 64-byte raw-string limit. The parser should
            // concatenate them.
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("ChunkedToken"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Has a long IPFS hash"));

            ListPlutusData chunks = new ListPlutusData();
            chunks.add(BytesPlutusData.of("ipfs://QmFirstChunk"));
            chunks.add(BytesPlutusData.of("AndSecondChunkConcat"));
            properties.put(BytesPlutusData.of("image"), chunks);

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().image()).isEqualTo("ipfs://QmFirstChunkAndSecondChunkConcat");
        }

        @Test
        void shouldExtractFilesArrayIntoPropertiesJson() throws Exception {
            // files[]: list of {name, mediaType, src} maps. Ends up under
            // properties["files"] in the JSONB column.
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("MultiMediaNft"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Has multiple files"));

            MapPlutusData file1 = new MapPlutusData();
            file1.put(BytesPlutusData.of("name"), BytesPlutusData.of("main"));
            file1.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));
            file1.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://QmMain"));

            MapPlutusData file2 = new MapPlutusData();
            file2.put(BytesPlutusData.of("name"), BytesPlutusData.of("hires"));
            file2.put(BytesPlutusData.of("mediaType"), BytesPlutusData.of("image/png"));
            file2.put(BytesPlutusData.of("src"), BytesPlutusData.of("ipfs://QmHires"));

            ListPlutusData filesList = new ListPlutusData();
            filesList.add(file1);
            filesList.add(file2);
            properties.put(BytesPlutusData.of("files"), filesList);

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().properties()).isNotNull();
            assertThat(result.get().properties()).containsKey("files");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> files =
                    (java.util.List<java.util.Map<String, Object>>) result.get().properties().get("files");
            assertThat(files).hasSize(2);
            assertThat(files.get(0)).containsEntry("name", "main")
                    .containsEntry("mediaType", "image/png")
                    .containsEntry("src", "ipfs://QmMain");
            assertThat(files.get(1)).containsEntry("name", "hires");
        }

        @Test
        void shouldCaptureUnknownPropertiesUnderAdditionalProperties() throws Exception {
            // Project-specific keys (attributes, traits) should land under
            // properties["additional_properties"], not be silently dropped.
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("AttributeToken"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Has custom traits"));
            properties.put(BytesPlutusData.of("rarity"), BytesPlutusData.of("legendary"));
            properties.put(BytesPlutusData.of("level"), BigIntPlutusData.of(42));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().properties()).isNotNull();
            assertThat(result.get().properties()).containsKey("additional_properties");
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> additional =
                    (java.util.Map<String, Object>) result.get().properties().get("additional_properties");
            assertThat(additional).containsEntry("rarity", "legendary");
            assertThat(additional.get("level")).isEqualTo(java.math.BigInteger.valueOf(42));
        }

        @Test
        void shouldReturnNullPropertiesWhenNoFilesNoAdditionalProperties() throws Exception {
            // Pure FT-shape datum with only well-known keys → properties JSONB is null
            // (no need to materialise an empty wrapper).
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("PureFt"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Just an FT"));
            properties.put(BytesPlutusData.of("ticker"), BytesPlutusData.of("PFT"));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().properties()).isNull();
        }
    }

    @Nested
    class ParseInvalidDatum {

        @Test
        void shouldReturnEmptyForNullInput() {
            Optional<ParsedCip68Datum> result = parser.parse(null);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForEmptyString() {
            Optional<ParsedCip68Datum> result = parser.parse("");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForBlankString() {
            Optional<ParsedCip68Datum> result = parser.parse("   ");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForInvalidHex() {
            Optional<ParsedCip68Datum> result = parser.parse("not-valid-hex");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNotConstrPlutusData() throws Exception {
            MapPlutusData mapData = new MapPlutusData();
            mapData.put(BytesPlutusData.of("name"), BytesPlutusData.of("Test"));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(mapData.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenDataListTooSmall() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Test"));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties);

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenFirstElementIsNotMap() throws Exception {
            ConstrPlutusData datum = ConstrPlutusData.of(0,
                    BytesPlutusData.of("not-a-map"),
                    BigIntPlutusData.of(1));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenVersionIsNotBigInt() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Test"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));

            ConstrPlutusData datum = ConstrPlutusData.of(0,
                    properties,
                    BytesPlutusData.of("not-a-number"));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<ParsedCip68Datum> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }
    }

}
