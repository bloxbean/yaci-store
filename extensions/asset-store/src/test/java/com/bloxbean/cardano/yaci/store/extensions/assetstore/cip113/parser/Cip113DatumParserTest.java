package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cip113RegistryNodeParser")
class Cip113DatumParserTest {

    private final Cip113RegistryNodeParser parser = new Cip113RegistryNodeParser();

    @Nested
    @DisplayName("Valid datums")
    class ValidDatums {

        @Test
        void parsesFullRegistryNode() throws Exception {
            String inlineDatum = buildDatum(
                    "0befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d1",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                    "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                    "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                    "1234567890abcdef1234567890abcdef1234567890abcdef12345678"
            );

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(inlineDatum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo("0befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d1");
            assertThat(result.get().next()).isEqualTo("ffffffffffffffffffffffffffffffffffffffffffffffffffffff");
            assertThat(result.get().transferLogicScript()).isEqualTo("aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102");
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103");
            assertThat(result.get().globalStatePolicyId()).isEqualTo("1234567890abcdef1234567890abcdef1234567890abcdef12345678");
        }

        @Test
        void parsesSentinelNode() throws Exception {
            String inlineDatum = buildDatum(
                    "",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                    "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                    "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                    ""
            );

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(inlineDatum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEmpty();
            assertThat(result.get().globalStatePolicyId()).isNull();
            assertThat(result.get().next()).isEqualTo("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        }

        @Test
        void parsesNodeWithoutGlobalState() throws Exception {
            ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                    BytesPlutusData.of(HexUtil.decodeHexString("deadbeef")),
                    BytesPlutusData.of(HexUtil.decodeHexString("cafebabe")),
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString("aabbccdd"))),
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString("11223344")))
            );
            String inlineDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(inlineDatum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo("deadbeef");
            assertThat(result.get().transferLogicScript()).isEqualTo("aabbccdd");
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("11223344");
            assertThat(result.get().globalStatePolicyId()).isNull();
        }
    }

    @Nested
    @DisplayName("Invalid inputs")
    class InvalidInputs {

        @Test
        void returnsEmptyForInvalidHex() {
            assertThat(parser.parse("invalid_hex")).isEmpty();
        }

        @Test
        void returnsEmptyForNull() {
            assertThat(parser.parse(null)).isEmpty();
        }

        @Test
        void returnsEmptyForBlank() {
            assertThat(parser.parse("  ")).isEmpty();
        }

        @Test
        void returnsEmptyWhenTransferLogicScriptMissing() throws Exception {
            ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                    BytesPlutusData.of(HexUtil.decodeHexString("deadbeef")),
                    BytesPlutusData.of(HexUtil.decodeHexString("cafebabe")),
                    BytesPlutusData.of(new byte[0]),  // not wrapped in Constr — extractCredentialBytes returns null
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString("11223344")))
            );
            String inlineDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));

            assertThat(parser.parse(inlineDatum)).isEmpty();
        }

        @Test
        void returnsEmptyWhenThirdPartyLogicScriptMissing() throws Exception {
            ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                    BytesPlutusData.of(HexUtil.decodeHexString("deadbeef")),
                    BytesPlutusData.of(HexUtil.decodeHexString("cafebabe")),
                    ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString("aabbccdd"))),
                    BytesPlutusData.of(new byte[0])  // not wrapped in Constr — extractCredentialBytes returns null
            );
            String inlineDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));

            assertThat(parser.parse(inlineDatum)).isEmpty();
        }
    }

    private static String buildDatum(String key, String next, String transferLogic,
                                      String thirdPartyLogic, String globalState) throws Exception {
        ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                BytesPlutusData.of(key.isEmpty() ? new byte[0] : HexUtil.decodeHexString(key)),
                BytesPlutusData.of(HexUtil.decodeHexString(next)),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(transferLogic))),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(thirdPartyLogic))),
                BytesPlutusData.of(globalState.isEmpty() ? new byte[0] : HexUtil.decodeHexString(globalState))
        );
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));
    }

}
