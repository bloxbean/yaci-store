package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.ParsedCip68Datum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CIP-68 defines the datum as {@code #6.121([metadata, version, extra])}, constructor 0. A datum of another constructor is
 * read as written, with a MANDATORY {@code invalid_constructor} error (ADR 0006): on mainnet 12 datums are not constructor
 * 0, among them two of BuyBTC (a 333 token, constructor 1) and one of Greenland Reserve Coin (constructor 2400616764, in
 * the general form, tag 102).
 */
class Cip68DatumConstructorTest {

    private static final AssetType BUY_BTC = new AssetType("b0e4da62dd435786d6875fa0ae1a62b8b6d8460c50b64a7d5f8313aa", "000643b0427579425443");
    /** Mainnet: BuyBTC, slot 156443701, constructor 1 (d87a). */
    private static final String BUY_BTC_DATUM = "d87a9fa4446e616d6546427579425443467469636b65724642757942544348646563696d616c7301"
            + "4375726c5768747470733a2f2f6275792d6274632e66696e616e636501ff";

    private static final AssetType GNRC = new AssetType("67cee89d59ab5354ee22c8af0638224126aecc6210f9372a61f13a64", "000643b0474e5243");
    /** Mainnet: Greenland Reserve Coin, slot 167655581, constructor 2400616764 in the general form (d866). */
    private static final String GNRC_DATUM = "d866821a8f16813c9fa6446e616d6556477265656e6c616e64205265736572766520436f696e4b"
            + "6465736372697074696f6e583c4173736574206261636b656420746f6b656e207365637572656420627920477265656e6c616e64207275"
            + "6269657320616e6420736170706869726573467469636b657244474e52434375726c582368747470733a2f2f7777772e7468652d6d69"
            + "6e742e636f6d2f636f6d706c69616e6365446c6f676f4048646563696d616c73410600d866821a951b3c2b9f81581cc0bb241d37ffbdfd"
            + "bb07d3d34bff54671c00935128da06966bc033810102ffff";

    private final Cip68DatumParser parser = new Cip68DatumParser();

    @Test
    void readsTheRealMainnetConstructor1DatumAsWrittenWithAMandatoryError() {
        ParsedCip68Datum parsed = parser.parse(BUY_BTC_DATUM, BUY_BTC).orElseThrow();

        assertThat(parsed.name()).isEqualTo("BuyBTC");
        assertThat(parsed.ticker()).isEqualTo("BuyBTC");
        assertThat(parsed.version()).isEqualTo(1L);
        assertThat(parsed.validationErrors()).singleElement().satisfies(e -> {
            assertThat(e.fieldName()).isEqualTo("constructor");
            assertThat(e.fieldCategory().name()).isEqualTo("MANDATORY");
            assertThat(e.code()).isEqualTo(Cip68DatumParser.INVALID_CONSTRUCTOR);
            assertThat(e.error()).contains("constructor 1");
        });
    }

    @Test
    void readsTheRealMainnetGeneralFormConstructorDatumAsWrittenWithTheError() {
        ParsedCip68Datum parsed = parser.parse(GNRC_DATUM, GNRC).orElseThrow();

        assertThat(parsed.name()).isEqualTo("Greenland Reserve Coin");
        assertThat(parsed.validationErrors()).extracting(ValidationError::code).contains(Cip68DatumParser.INVALID_CONSTRUCTOR);
        assertThat(parsed.validationErrors()).filteredOn(e -> Cip68DatumParser.INVALID_CONSTRUCTOR.equals(e.code()))
                .singleElement().satisfies(e -> assertThat(e.error()).contains("constructor 2400616764"));
    }

    @Test
    void aConstructor0DatumHasNoConstructorError() throws Exception {
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
        String datum = HexUtil.encodeHexString(CborSerializationUtil.serialize(
                ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));

        assertThat(parser.parse(datum).orElseThrow().validationErrors()).isEmpty();
    }
}
