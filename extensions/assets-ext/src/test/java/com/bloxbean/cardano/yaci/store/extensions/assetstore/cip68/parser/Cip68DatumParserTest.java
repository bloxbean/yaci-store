package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    class OutOfRangeDecimals {

        private Optional<ParsedCip68Datum> parseWithDecimals(BigInteger decimals) throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
            properties.put(BytesPlutusData.of("decimals"), BigIntPlutusData.of(decimals));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));

            return parser.parse(HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize())));
        }

        @Test
        void shouldKeepDecimalsAtUpperBound() throws Exception {
            assertThat(parseWithDecimals(BigInteger.valueOf(255)))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isEqualTo(255L));
        }

        @Test
        void shouldDropDecimalsAboveUpperBoundButKeepMetadata() throws Exception {
            assertThat(parseWithDecimals(BigInteger.valueOf(256))).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Token");
                assertThat(m.decimals()).isNull();
            });
        }

        @Test
        void shouldDropDecimalsBeyondIntRange() throws Exception {
            assertThat(parseWithDecimals(BigInteger.TWO.pow(40)))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isNull());
        }

        @Test
        void shouldDropDecimalsThatWouldWrapWhenNarrowedToLong() throws Exception {
            // 2^64 + 3: longValue() gives 3, which would otherwise look valid.
            assertThat(parseWithDecimals(BigInteger.TWO.pow(64).add(BigInteger.valueOf(3))))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isNull());
        }

        @Test
        void shouldDropNegativeDecimals() throws Exception {
            assertThat(parseWithDecimals(BigInteger.valueOf(-1)))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isNull());
        }
    }

    @Nested
    class OversizedValues {

        private Optional<ParsedCip68Datum> parse(String key, String value, BigInteger version) throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
            properties.put(BytesPlutusData.of(key), BytesPlutusData.of(value));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(version));

            return parser.parse(HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize())));
        }

        private Optional<ParsedCip68Datum> parse(String key, String value) throws Exception {
            return parse(key, value, BigInteger.ONE);
        }

        @Test
        void shouldKeepStringsAtColumnWidth() throws Exception {
            assertThat(parse("ticker", "T".repeat(32)))
                    .hasValueSatisfying(m -> assertThat(m.ticker()).hasSize(32));
            assertThat(parse("url", "u".repeat(250)))
                    .hasValueSatisfying(m -> assertThat(m.url()).hasSize(250));
            assertThat(parse("mediaType", "m".repeat(255)))
                    .hasValueSatisfying(m -> assertThat(m.mediaType()).hasSize(255));
        }

        @Test
        void shouldDropStringsWiderThanColumnButKeepMetadata() throws Exception {
            assertThat(parse("ticker", "T".repeat(33))).hasValueSatisfying(m -> {
                assertThat(m.ticker()).isNull();
                assertThat(m.name()).isEqualTo("Token");
            });
            assertThat(parse("url", "u".repeat(251)))
                    .hasValueSatisfying(m -> assertThat(m.url()).isNull());
            assertThat(parse("mediaType", "m".repeat(256)))
                    .hasValueSatisfying(m -> assertThat(m.mediaType()).isNull());
        }

        @Test
        void shouldDropOversizedNameSoDatumFailsRequiredFieldCheck() throws Exception {
            assertThat(parse("name", "N".repeat(256)))
                    .hasValueSatisfying(m -> assertThat(m.name()).isNull());
        }

        @Test
        void shouldCountUtf16UnitsNotCodePoints() throws Exception {
            // 128 emoji = 128 code points but 256 UTF-16 units, which H2 rejects for VARCHAR(255)
            assertThat(parse("name", "🚀".repeat(128)))
                    .hasValueSatisfying(m -> assertThat(m.name()).isNull());
            // 127 emoji = 254 UTF-16 units: fits on every database
            assertThat(parse("name", "🚀".repeat(127)))
                    .hasValueSatisfying(m -> assertThat(m.name()).hasSize(254));
        }

        @Test
        void shouldRejectDatumWhoseVersionWouldWrapWhenNarrowedToLong() throws Exception {
            // 2^64 + 1: longValue() gives 1, which would otherwise look like a valid version
            assertThat(parse("ticker", "TT", BigInteger.TWO.pow(64).add(BigInteger.ONE))).isEmpty();
        }

        @Test
        void shouldOnlyAcceptTheVersionsCip68Defines() throws Exception {
            for (long version = 1; version <= 4; version++) {
                final long v = version;
                assertThat(parse("ticker", "TT", BigInteger.valueOf(v)))
                        .hasValueSatisfying(m -> assertThat(m.version()).isEqualTo(v));
            }
            // anything else is not indexed, whether it fits a long or not
            assertThat(parse("ticker", "TT", BigInteger.ZERO)).isEmpty();
            assertThat(parse("ticker", "TT", BigInteger.valueOf(5))).isEmpty();
            assertThat(parse("ticker", "TT", BigInteger.valueOf(Long.MAX_VALUE))).isEmpty();
            assertThat(parse("ticker", "TT", BigInteger.TWO.pow(63))).isEmpty();
        }
    }

    @Nested
    class NestedMapFormat {

        private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
        private static final String OTHER_POLICY_ID = "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344";
        private static final String ASSET_NAME_HEX = HexUtil.encodeHexString("Token".getBytes());
        private static final AssetType REFERENCE_NFT = new AssetType(POLICY_ID, "000643b0" + ASSET_NAME_HEX);

        private static MapPlutusData metadata(String name) {
            MapPlutusData metadata = new MapPlutusData();
            metadata.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
            metadata.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
            return metadata;
        }

        /** {"721": {policy_id: {asset_name: metadata}}} with raw-byte policy and asset keys, per CIP-68 version 4. */
        private static MapPlutusData nested(String policyId, String assetNameHex, MapPlutusData metadata) {
            MapPlutusData byAsset = new MapPlutusData();
            byAsset.put(BytesPlutusData.of(HexUtil.decodeHexString(assetNameHex)), metadata);
            MapPlutusData byPolicy = new MapPlutusData();
            byPolicy.put(BytesPlutusData.of(HexUtil.decodeHexString(policyId)), byAsset);
            MapPlutusData root = new MapPlutusData();
            root.put(BytesPlutusData.of("721"), byPolicy);
            return root;
        }

        private static String datum(MapPlutusData properties, long version) throws Exception {
            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(version));
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
        }

        @Test
        void shouldReportANestedDatumAsNested() throws Exception {
            String hex = datum(nested(POLICY_ID, ASSET_NAME_HEX, metadata("Nested")), 4);

            assertThat(parser.hasNestedMetadata(hex)).isTrue();
        }

        @Test
        void shouldNotReportAFlatDatumAsNested() throws Exception {
            assertThat(parser.hasNestedMetadata(datum(metadata("Flat"), 1))).isFalse();
            // version 4 without the "721" key is read as a flat map
            assertThat(parser.hasNestedMetadata(datum(metadata("Flat v4"), 4))).isFalse();
        }

        @Test
        void shouldReportA721KeyAsNestedWhateverTheVersion() throws Exception {
            // the CIP's step 4 tells nested from direct metadata by the "721" key, not by the version
            for (long version : new long[]{1, 3, 4, 5, 100}) {
                assertThat(parser.hasNestedMetadata(datum(nested(POLICY_ID, ASSET_NAME_HEX, metadata("Any")), version)))
                        .as("version %d", version).isTrue();
            }
        }

        @Test
        void shouldNotReportAnythingThatIsNotACip68DatumAsNested() {
            assertThat(parser.hasNestedMetadata(null)).isFalse();
            assertThat(parser.hasNestedMetadata("")).isFalse();
            assertThat(parser.hasNestedMetadata("not-hex")).isFalse();
            assertThat(parser.hasNestedMetadata("d8799f00ff")).isFalse();
        }

        @Test
        void shouldResolveVersion4NestedMapForReferenceNft() throws Exception {
            String datum = datum(nested(POLICY_ID, ASSET_NAME_HEX, metadata("Nested")), 4);

            assertThat(parser.parse(datum, REFERENCE_NFT)).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Nested");
                assertThat(m.description()).isEqualTo("Desc");
                assertThat(m.version()).isEqualTo(4L);
                assertThat(m.properties()).isNull(); // the "721" wrapper is not leaked as an additional property
            });
        }

        @Test
        void shouldPickTheEntryForThisReferenceNftAmongSeveral() throws Exception {
            MapPlutusData root = nested(POLICY_ID, ASSET_NAME_HEX, metadata("Mine"));
            MapPlutusData byPolicy = (MapPlutusData) root.getMap().get(BytesPlutusData.of("721"));
            MapPlutusData byAsset = (MapPlutusData) byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(POLICY_ID)));
            byAsset.put(BytesPlutusData.of("Other".getBytes()), metadata("Other"));

            assertThat(parser.parse(datum(root, 4), REFERENCE_NFT))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Mine"));
        }

        @Test
        void shouldReturnEmptyWhenNestedMapHasNoEntryForReferenceNft() throws Exception {
            String datum = datum(nested(OTHER_POLICY_ID, ASSET_NAME_HEX, metadata("Foreign")), 4);

            assertThat(parser.parse(datum, REFERENCE_NFT)).isEmpty();
        }

        @Test
        void shouldResolveSingleEntryWithoutAssetContext() throws Exception {
            String datum = datum(nested(POLICY_ID, ASSET_NAME_HEX, metadata("Only")), 4);

            assertThat(parser.parse(datum)).hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Only"));
        }

        @Test
        void shouldReadVersion3DatumDirectlyEvenWithA721Key() throws Exception {
            // Before version 4 a "721" key is just an additional property, not a wrapper
            MapPlutusData properties = metadata("Direct");
            properties.put(BytesPlutusData.of("721"), BytesPlutusData.of("x"));

            assertThat(parser.parse(datum(properties, 3), REFERENCE_NFT)).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Direct");
                assertThat(m.properties()).containsKey("additional_properties");
            });
        }
    }

    @Nested
    class ChunkedLogo {

        @Test
        void shouldJoinLogoGivenAsListOfChunks() throws Exception {
            // CIP-68 FT: logo is a uri = bounded_bytes / [* bounded_bytes]
            String first = "data:image/png;base64,";
            String second = "iVBORw0KGgo=";
            ListPlutusData chunks = ListPlutusData.of(BytesPlutusData.of(first), BytesPlutusData.of(second));

            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
            properties.put(BytesPlutusData.of("logo"), chunks);
            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));

            assertThat(parser.parse(HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()))))
                    .hasValueSatisfying(m -> assertThat(m.logo()).isEqualTo(first + second));
        }
    }

    @Nested
    class DeeplyNestedDatum {

        /** {@code depth} nested single-element lists around 0, built as raw CBOR so building it doesn't recurse. */
        private String nestedDatumHex(int depth) {
            return "81".repeat(depth) + "00";
        }

        /** Parses on a 1 MB stack, the default on Linux x86_64, and returns the result or what was thrown. */
        private Object parseOnDefaultLinuxStack(String datumHex) throws InterruptedException {
            AtomicReference<Object> outcome = new AtomicReference<>();
            Thread thread = new Thread(null, () -> {
                try {
                    outcome.set(parser.parse(datumHex));
                } catch (Throwable t) {
                    outcome.set(t);
                }
            }, "deep-datum", 1024 * 1024);
            thread.start();
            thread.join();
            return outcome.get();
        }

        @Test
        void shouldSkipDatumNestedTooDeeplyToDecode() throws Exception {
            // 5,000 levels overflows the current decoder on a 1 MB stack; 16,000 is about the most a
            // 16 KB transaction can carry. This stays valid once the decoder is stack-safe
            // (cardano-client-lib#681): the datum isn't a CIP-68 constructor, so it is skipped either way.
            for (int depth : new int[]{5_000, 16_000}) {
                assertThat(parseOnDefaultLinuxStack(nestedDatumHex(depth)))
                        .as("depth %d", depth)
                        .isEqualTo(Optional.empty());
            }
        }
    }

    @Nested
    class AdditionalPropertyValues {

        /** Preprod NFT from bloxbean/yaci-store#1159: {@code contractData} is a Plutus constructor. */
        private static final String NFT_WITH_CONSTRUCTOR_PROPERTY =
                "d87982a3446e616d65464e465420233145696d616765404c636f6e747261637444617461d879860181581cdc9acfee35"
                + "243d123e8f10bc58692a6bc5aa3135c7eafc2aac9daafcd87a80581c9abc17656a6d1c24688292777c18c1ce599845a5"
                + "88f4d893c1884da2d87a80d87a8001";

        /** Preprod fungible token (Wrapped pUSDC): {@code seed} is a constructor, {@code oracles} are key hashes. */
        private static final String FT_WITH_CONSTRUCTOR_PROPERTY =
                "d8799fae46737570706c791a1a449c8b44747970654c5772617070656441737365744576656e75654845746865726575"
                + "6d46706f6c696379582a3078413062383639393163363231386233366331643139443461326539456230634533363036"
                + "65423438476163636f756e74582a30783732423530393631423237343734624433363241436346393638616630316633"
                + "6338323863336432467469636b6572457055534443446e616d654d577261707065642070555344434b64657363726970"
                + "74696f6e582057726170706564207055534443206f70657261746564206279205042472e696f48646563696d616c7306"
                + "4375726c4e68747470733a2f2f7062672e696f446c6f676f582868747470733a2f2f70726570726f642e706267746f6b"
                + "656e2e696f2f7277612d6c6f676f2e706e674671756f72756d02476f7261636c65739f581c80edfa909a3d40a54fca4c"
                + "3ee852c7ba2a79391738911dc363580dc2581cab25d3b9476a3e3343a2f353b08b40913c573de7d286ef37ac4013e058"
                + "1c7bd1ebc8230f961193fb772204542e85425af4f7a8f36acb5543da08ff4473656564d8799fd8799f582042f5390b27"
                + "9a4b49d56fe594b2d5eaf02e8e387fa1612f87bafd2feed7c836afff02ff01d87980ff";

        /**
         * Mainnet "PBG Token Voucher" datums that {@code main} dropped (the same NullPointerException as
         * #1159): {@code owner} is a constructor. Three picked at random from the 434 distinct datums seen
         * in a full mainnet sync, keyed by hex with the expected name.
         */
        private static final Map<String, String> MAINNET_VOUCHERS_WITH_CONSTRUCTOR_OWNER = Map.of(
                "d8799fa9456f776e6572d8799fd8799f581c3e8242c26c999d22ecfc760cdc7a34508637c4351b3187ee20eb77ebffd8"
                + "7a80ff45646174756d0046746f6b656e731a001cf2c046706572696f64034570726963659f1b0000023fff1df4ed1b00"
                + "000005242abee5ff446e616d655450424720546f6b656e20566f75636865722038394b6465736372697074696f6e5821"
                + "5375636365737320666565207265696d62757273656d656e7420766f75636865724375726c5768747470733a2f2f7062"
                + "672e696f2f766f75636865727345696d616765582568747470733a2f2f746f6b656e2e7062672e696f2f766f75636865"
                + "722d6c6f676f2e706e6701d87980ff",
                "PBG Token Voucher 89",
                "d8799fa9456f776e6572d8799fd8799f581c90c98fa2c409dcaf2cec7770e8a614df2322fb08c545ddae0b285209ffd8"
                + "799fd8799fd8799f581cebffd86359482e97a1d7755cdae86739ffcafa6033713b735c4f4031ffffffff45646174756d"
                + "0046746f6b656e731b00000001ea0659e846706572696f64014570726963659f1b00000001c09103241a04748ab2ff44"
                + "6e616d655350424720546f6b656e20566f756368657220394b6465736372697074696f6e582153756363657373206665"
                + "65207265696d62757273656d656e7420766f75636865724375726c5768747470733a2f2f7062672e696f2f766f756368"
                + "65727345696d616765582568747470733a2f2f746f6b656e2e7062672e696f2f766f75636865722d6c6f676f2e706e67"
                + "01d87980ff",
                "PBG Token Voucher 9",
                "d8799fa9456f776e6572d8799fd8799f581ce29e15bf7d0deaabdb963a1f9ffc9adc661dbf6889ae73a17ae39843ffd8"
                + "799fd8799fd8799f581c8b4e2900167bf48e98803bb0ed0cd7c2f6b699a08716e8a520b507f9ffffffff45646174756d"
                + "0046746f6b656e731a00b1505246706572696f64044570726963659f1b0000029ff9d4b0d31b00000005d2138378ff44"
                + "6e616d655550424720546f6b656e20566f7563686572203132384b6465736372697074696f6e58215375636365737320"
                + "666565207265696d62757273656d656e7420766f75636865724375726c5768747470733a2f2f7062672e696f2f766f75"
                + "636865727345696d616765582568747470733a2f2f746f6b656e2e7062672e696f2f766f75636865722d6c6f676f2e70"
                + "6e6701d87980ff",
                "PBG Token Voucher 128");

        @SuppressWarnings("unchecked")
        private static Map<String, Object> additionalProperties(ParsedCip68Datum datum) {
            return (Map<String, Object>) datum.properties().get("additional_properties");
        }

        @Test
        void shouldKeepTheTokenAndDropThePropertyWhenItIsAConstructor() {
            assertThat(parser.parse(NFT_WITH_CONSTRUCTOR_PROPERTY)).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("NFT #1");
                // contractData was the only additional property
                assertThat(m.properties()).isNull();
            });
        }

        @Test
        void shouldKeepFungibleTokenMetadataAndDropOnlyTheConstructorProperty() {
            assertThat(parser.parse(FT_WITH_CONSTRUCTOR_PROPERTY)).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Wrapped pUSDC");
                assertThat(m.ticker()).isEqualTo("pUSDC");
                assertThat(m.decimals()).isEqualTo(6L);

                Map<String, Object> additional = additionalProperties(m);
                assertThat(additional).doesNotContainKey("seed");
                assertThat(additional.get("oracles")).isEqualTo(List.of(
                        "80edfa909a3d40a54fca4c3ee852c7ba2a79391738911dc363580dc2",
                        "ab25d3b9476a3e3343a2f353b08b40913c573de7d286ef37ac4013e0",
                        "7bd1ebc8230f961193fb772204542e85425af4f7a8f36acb5543da08"));
                assertThat(additional.get("venue")).isEqualTo("Ethereum");
            });
        }

        @Test
        void shouldKeepMainnetVouchersAndDropTheirConstructorOwner() {
            MAINNET_VOUCHERS_WITH_CONSTRUCTOR_OWNER.forEach((datum, name) ->
                    assertThat(parser.parse(datum)).as(name).hasValueSatisfying(m -> {
                        assertThat(m.name()).isEqualTo(name);
                        assertThat(m.version()).isEqualTo(1L);
                        assertThat(m.url()).isEqualTo("https://pbg.io/vouchers");
                        assertThat(additionalProperties(m)).containsKeys("datum", "tokens", "period", "price")
                                .doesNotContainKey("owner");
                    }));
        }

        @Test
        void shouldStoreUtf8BytesAsTextAndOtherBytesAsHex() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
            properties.put(BytesPlutusData.of("label"), BytesPlutusData.of("caf\u00e9"));
            properties.put(BytesPlutusData.of("hash"), BytesPlutusData.of(new byte[]{(byte) 0xff, 0x00, (byte) 0xc3}));
            ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));

            assertThat(parser.parse(HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()))))
                    .hasValueSatisfying(m -> {
                        assertThat(additionalProperties(m).get("label")).isEqualTo("caf\u00e9");
                        assertThat(additionalProperties(m).get("hash")).isEqualTo("ff00c3");
                    });
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
