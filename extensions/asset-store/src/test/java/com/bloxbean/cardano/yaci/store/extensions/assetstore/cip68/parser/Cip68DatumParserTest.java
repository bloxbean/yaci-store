package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68TokenMetadata;
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

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            Cip68TokenMetadata metadata = result.get();
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

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            Cip68TokenMetadata metadata = result.get();
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

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("TestToken");
            assertThat(result.get().description()).isEqualTo("Adesc");
        }
    }

    @Nested
    class ParseInvalidDatum {

        @Test
        void shouldReturnEmptyForNullInput() {
            Optional<Cip68TokenMetadata> result = parser.parse(null);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForEmptyString() {
            Optional<Cip68TokenMetadata> result = parser.parse("");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForBlankString() {
            Optional<Cip68TokenMetadata> result = parser.parse("   ");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForInvalidHex() {
            Optional<Cip68TokenMetadata> result = parser.parse("not-valid-hex");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenNotConstrPlutusData() throws Exception {
            MapPlutusData mapData = new MapPlutusData();
            mapData.put(BytesPlutusData.of("name"), BytesPlutusData.of("Test"));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(mapData.serialize()));

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenDataListTooSmall() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Test"));

            ConstrPlutusData datum = ConstrPlutusData.of(0, properties);

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenFirstElementIsNotMap() throws Exception {
            ConstrPlutusData datum = ConstrPlutusData.of(0,
                    BytesPlutusData.of("not-a-map"),
                    BigIntPlutusData.of(1));

            String hexDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

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

            Optional<Cip68TokenMetadata> result = parser.parse(hexDatum);

            assertThat(result).isEmpty();
        }
    }

}
