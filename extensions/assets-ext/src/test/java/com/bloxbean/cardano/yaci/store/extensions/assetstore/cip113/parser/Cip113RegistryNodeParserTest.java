package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S2187") // tests are in @Nested inner classes
@DisplayName("Cip113RegistryNodeParser")
class Cip113RegistryNodeParserTest {

    // All hash-shaped values are 56 hex chars = 28 bytes (Blake2b-224 / policy id).
    // Each credential slot gets a distinct value so a shifted field mapping cannot pass.
    private static final String POLICY_A_HEX = "0befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d1";
    private static final String POLICY_B_HEX = "1befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d2";
    private static final String CRED_MINTING_HEX = "999513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126101";
    private static final String CRED_TRANSFER_HEX = "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102";
    private static final String CRED_THIRD_PARTY_HEX = "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103";
    private static final String CRED_UNFRACKING_HEX = "bbb513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126104";
    private static final String GLOBAL_STATE_POLICY_HEX = "1234567890abcdef1234567890abcdef1234567890abcdef12345678";

    /** Tail sentinel conventionally ~32 bytes of 0xFF in the aiken-linked-list library. */
    private static final byte[] TAIL_SENTINEL_32B = new byte[32];

    static {
        java.util.Arrays.fill(TAIL_SENTINEL_32B, (byte) 0xFF);
    }

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final Cip113RegistryNodeParser parser = new Cip113RegistryNodeParser();

    // ----- Happy path -----------------------------------------------------------------

    @Nested
    @DisplayName("Valid datums")
    class ValidDatums {

        @Test
        void mapsEachFieldFromItsPosition() throws Exception {
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    hex(GLOBAL_STATE_POLICY_HEX)));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).hasValue(new Cip113RegistryNodeParser.ParsedRegistryNode(
                    POLICY_A_HEX,
                    POLICY_B_HEX,
                    CRED_MINTING_HEX,
                    CRED_TRANSFER_HEX,
                    CRED_THIRD_PARTY_HEX,
                    CRED_UNFRACKING_HEX,
                    GLOBAL_STATE_POLICY_HEX));
        }

        @Test
        void parsesNodeWithScriptCredentials() throws Exception {
            // Credential alternative 1 = Script (vs 0 = VerificationKey). Both are valid.
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    scriptCred(hex(CRED_MINTING_HEX)),
                    scriptCred(hex(CRED_TRANSFER_HEX)),
                    scriptCred(hex(CRED_THIRD_PARTY_HEX)),
                    scriptCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            // Note: the VKey vs Script variant is intentionally discarded — both surface as the same hex.
            assertThat(result.get().mintingLogicScript()).isEqualTo(CRED_MINTING_HEX);
            assertThat(result.get().transferLogicScript()).isEqualTo(CRED_TRANSFER_HEX);
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo(CRED_THIRD_PARTY_HEX);
            assertThat(result.get().unfrackingLogicScript()).isEqualTo(CRED_UNFRACKING_HEX);
        }

        @Test
        void parsesHeadSentinelNode() throws Exception {
            // The origin node: key empty, every credential empty_vkey, no global state.
            String datum = serialize(node(
                    EMPTY_BYTES,
                    hex(POLICY_A_HEX),
                    vkeyCred(EMPTY_BYTES),
                    vkeyCred(EMPTY_BYTES),
                    vkeyCred(EMPTY_BYTES),
                    vkeyCred(EMPTY_BYTES),
                    EMPTY_BYTES));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEmpty();
            assertThat(result.get().next()).isEqualTo(POLICY_A_HEX);
            assertThat(result.get().globalStatePolicyId()).isNull();
        }

        @Test
        void parsesNodeWithAbsentGlobalState() throws Exception {
            // global_state_cs = empty bytes → semantically absent → null in the parsed output.
            String datum = buildDatum(hex(POLICY_A_HEX), hex(POLICY_B_HEX), EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().globalStatePolicyId()).isNull();
        }

        @Test
        void parsesNodeWithMaxLengthNextSentinel() throws Exception {
            // A materialized tail sentinel node: next points to the 32-byte 0xFF sentinel.
            String datum = buildDatum(hex(POLICY_A_HEX), TAIL_SENTINEL_32B, EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().next()).isEqualTo("ff".repeat(32));
        }

        @Test
        void parsesNodeWithAbsentTransferLogicScriptAsPlainEmptyBytes() throws Exception {
            // Real-world tolerance: plain BytesPlutusData(h'') in place of a Credential Constr
            // means "no transfer_logic_script".
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isNull();
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo(CRED_THIRD_PARTY_HEX);
        }

        @Test
        void parsesNodeWithAbsentTransferLogicScriptAsWrappedEmptyBytes() throws Exception {
            // Also tolerate a Credential Constr whose inner ByteString is empty (empty_vkey).
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(EMPTY_BYTES),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isNull();
        }

        @Test
        void parsesNodeWithAbsentUnfrackingLogicScript() throws Exception {
            // empty_vkey unfracking credential = unfracking forbidden → null.
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(EMPTY_BYTES),
                    EMPTY_BYTES));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().unfrackingLogicScript()).isNull();
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo(CRED_THIRD_PARTY_HEX);
        }

        @Test
        void parsesNodeWithAllOptionalFieldsAbsent() throws Exception {
            // All five optional fields (four credentials + global_state_cs) can be simultaneously
            // null — only 'key' and 'next' are strictly required.
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(EMPTY_BYTES),
                    EMPTY_BYTES));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).hasValue(new Cip113RegistryNodeParser.ParsedRegistryNode(
                    POLICY_A_HEX, POLICY_B_HEX, null, null, null, null, null));
        }
    }

    // ----- Real deployed datums ---------------------------------------------------------

    /**
     * Inline datums of registry-node UTxOs from the official CIP-113 deployments (cip113-programmable-tokens),
     * fetched from mainnet, preprod and preview.
     */
    @Nested
    @DisplayName("Datums from the official mainnet, preprod and preview deployments")
    class DeployedDatums {

        /** Mainnet head node (registry policy 484e733d…0e3075): every credential is empty_vkey. */
        private static final String MAINNET_HEAD_NODE =
                "d8799f40581c01c24df7941f8b5856762fcc8aa0bb61a8c24f0911ed6aef474034d0d8799f40ffd8799f40ffd8799f40"
                + "ffd8799f40ff40ff";

        /** Mainnet node for policy b025efe5…: Script credentials, unfracking forbidden, global state set. */
        private static final String MAINNET_TOKEN_NODE =
                "d8799f581cb025efe5b44b43ed154419c66b0efc9cf148fd98f464e261018c89e8581effffffffffffffffffffffffff"
                + "ffffffffffffffffffffffffffffffffffd87a9f581cf462a4e22e5b138c17d893d4e0811790f51f1231b8be4198313e"
                + "08d0ffd87a9f581c62b98b0894870aadfba1a7fd1b368c56aa91caa2701fb7e92a3f1bf1ffd87a9f581c1e17ac8b06f5"
                + "f891d65923dee46e85a71e19deb401c8650e0401a3f0ffd8799f40ff581cd2683a1b0b9bf3628db24fdf7cababb08308"
                + "4aec86a0cfd05b130ca3ff";

        /** Preprod node for policy ba748823… (registry policy 3083d387…09222a): all four credentials set. */
        private static final String PREPROD_TOKEN_NODE =
                "d8799f581cba74882344a7946a04139263c4896703a7107f522bbf061c311aac6c581cc4e75d70e0df490e4f20868188"
                + "9f62615bc19a89490f81caa39e86dbd87a9f581cf91c65ec585a20176feeb2c4e5281e856d3b61e95d1980717da724fc"
                + "ffd87a9f581c0f948cd223933dfa2cf584e674f128e2baa7875cca2b8fe65aa6b1eaffd87a9f581cf91c65ec585a2017"
                + "6feeb2c4e5281e856d3b61e95d1980717da724fcffd87a9f581cf91c65ec585a20176feeb2c4e5281e856d3b61e95d19"
                + "80717da724fcff40ff";

        /** Preview node for policy ecfe3319… (registry policy e5b339ef…fec09a): unfracking forbidden. */
        private static final String PREVIEW_TOKEN_NODE =
                "d8799f581cecfe3319f2162b7206f186bf47ce27592dd2e96f87c8f8c83c71e6a2581cfade8905bc06f0f30b44175e3f"
                + "c776c7233e41855adf8df1be7ab5d3d87a9f581c0efb02aa36b3e6167c3f450249c2f9bbaf2d49efe80b930e1fb8e11e"
                + "ffd87a9f581c108f11b59a5afbaac5451bd86e22f002ebafc23d93d01f542ce93f4effd87a9f581c24bb10207c62baea"
                + "e9d83cfedcd515118c97fc7b3058f5c8006e49bfffd8799f40ff581c150ee5da3e245ea055dcc11e324d9fae8b7c136d"
                + "08b32f7e57b84663ff";

        @Test
        void parsesMainnetHeadNode() {
            assertThat(parser.parse(MAINNET_HEAD_NODE)).hasValue(new Cip113RegistryNodeParser.ParsedRegistryNode(
                    "",
                    "01c24df7941f8b5856762fcc8aa0bb61a8c24f0911ed6aef474034d0",
                    null, null, null, null, null));
        }

        @Test
        void parsesMainnetTokenNode() {
            assertThat(parser.parse(MAINNET_TOKEN_NODE)).hasValue(new Cip113RegistryNodeParser.ParsedRegistryNode(
                    "b025efe5b44b43ed154419c66b0efc9cf148fd98f464e261018c89e8",
                    "ff".repeat(30),
                    "f462a4e22e5b138c17d893d4e0811790f51f1231b8be4198313e08d0",
                    "62b98b0894870aadfba1a7fd1b368c56aa91caa2701fb7e92a3f1bf1",
                    "1e17ac8b06f5f891d65923dee46e85a71e19deb401c8650e0401a3f0",
                    null,
                    "d2683a1b0b9bf3628db24fdf7cababb083084aec86a0cfd05b130ca3"));
        }

        @Test
        void parsesPreprodTokenNode() {
            assertThat(parser.parse(PREPROD_TOKEN_NODE)).hasValue(new Cip113RegistryNodeParser.ParsedRegistryNode(
                    "ba74882344a7946a04139263c4896703a7107f522bbf061c311aac6c",
                    "c4e75d70e0df490e4f208681889f62615bc19a89490f81caa39e86db",
                    "f91c65ec585a20176feeb2c4e5281e856d3b61e95d1980717da724fc",
                    "0f948cd223933dfa2cf584e674f128e2baa7875cca2b8fe65aa6b1ea",
                    "f91c65ec585a20176feeb2c4e5281e856d3b61e95d1980717da724fc",
                    "f91c65ec585a20176feeb2c4e5281e856d3b61e95d1980717da724fc",
                    null));
        }

        @Test
        void parsesPreviewTokenNode() {
            assertThat(parser.parse(PREVIEW_TOKEN_NODE)).hasValue(new Cip113RegistryNodeParser.ParsedRegistryNode(
                    "ecfe3319f2162b7206f186bf47ce27592dd2e96f87c8f8c83c71e6a2",
                    "fade8905bc06f0f30b44175e3fc776c7233e41855adf8df1be7ab5d3",
                    "0efb02aa36b3e6167c3f450249c2f9bbaf2d49efe80b930e1fb8e11e",
                    "108f11b59a5afbaac5451bd86e22f002ebafc23d93d01f542ce93f4e",
                    "24bb10207c62baeae9d83cfedcd515118c97fc7b3058f5c8006e49bf",
                    null,
                    "150ee5da3e245ea055dcc11e324d9fae8b7c136d08b32f7e57b84663"));
        }
    }

    // ----- Root-structure invariants --------------------------------------------------

    @Nested
    @DisplayName("Root structure invariants")
    class RootStructureInvariants {

        @Test
        void rejectsNonConstrRoot() throws Exception {
            // A bare BytesPlutusData as the whole datum.
            String datum = serialize(BytesPlutusData.of(hex(POLICY_A_HEX)));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsListPlutusDataRoot() throws Exception {
            ListPlutusData list = ListPlutusData.of(
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)));
            String datum = serialize(list);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsWrongConstructorAlternative() throws Exception {
            // Alternative 1 with otherwise valid fields — must be rejected (RegistryNode is alternative 0).
            ConstrPlutusData node = ConstrPlutusData.of(1,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));

            assertThat(parser.parse(serialize(node))).isEmpty();
        }

        @Test
        void rejectsFewerThanSevenFields() throws Exception {
            // 6 fields — missing global_state_cs.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)));

            assertThat(parser.parse(serialize(node))).isEmpty();
        }

        @Test
        void rejectsMoreThanSevenFields() throws Exception {
            // 8 fields — an extra unknown trailing field.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(hex("cafe")));

            assertThat(parser.parse(serialize(node))).isEmpty();
        }
    }

    // ----- key / next field invariants ------------------------------------------------

    @Nested
    @DisplayName("key / next field invariants")
    class KeyNextInvariants {

        @Test
        void rejectsKeyAsNonByteString() throws Exception {
            // key is a Constr — wrong type.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    vkeyCred(hex(POLICY_A_HEX)),  // wrong type for field 0
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));

            assertThat(parser.parse(serialize(node))).isEmpty();
        }

        @Test
        void rejectsKeyExceedingMaxLength() throws Exception {
            // 33-byte key — exceeds the 32-byte cap (max sentinel convention).
            byte[] tooLong = new byte[33];
            java.util.Arrays.fill(tooLong, (byte) 0xFF);

            assertThat(parser.parse(buildDatum(tooLong, hex(POLICY_B_HEX), EMPTY_BYTES))).isEmpty();
        }

        @Test
        void rejectsEmptyNext() throws Exception {
            // next must be non-empty — an empty next would violate key < next for any key.
            assertThat(parser.parse(buildDatum(hex(POLICY_A_HEX), EMPTY_BYTES, EMPTY_BYTES))).isEmpty();
        }

        @Test
        void rejectsNextAsNonByteString() throws Exception {
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    vkeyCred(hex(POLICY_B_HEX)),  // wrong type for field 1
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));

            assertThat(parser.parse(serialize(node))).isEmpty();
        }

        @Test
        void rejectsNextExceedingMaxLength() throws Exception {
            byte[] tooLong = new byte[33];
            java.util.Arrays.fill(tooLong, (byte) 0xFF);

            assertThat(parser.parse(buildDatum(hex(POLICY_A_HEX), tooLong, EMPTY_BYTES))).isEmpty();
        }
    }

    // ----- Credential field invariants ------------------------------------------------

    @Nested
    @DisplayName("Credential field invariants")
    class CredentialInvariants {

        @Test
        void rejectsTransferLogicScriptAsPlainBytes() throws Exception {
            // Non-empty plain BytesPlutusData instead of a Credential Constr. Empty plain bytes are
            // tolerated as "absent credential"; non-empty are malformed.
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    BytesPlutusData.of(hex(CRED_TRANSFER_HEX)),  // not wrapped in Credential Constr
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsTransferLogicScriptWithWrongByteLength() throws Exception {
            // Credential wraps 10 bytes instead of 28.
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex("aabbccddeeff00112233")),  // 10 bytes
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsTransferLogicScriptWithInvalidAlternative() throws Exception {
            // Aiken Credential alternatives are {0=VerificationKey, 1=Script}. Anything else is invalid.
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    ConstrPlutusData.of(2, BytesPlutusData.of(hex(CRED_TRANSFER_HEX))),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsTransferLogicScriptWithMultipleInnerFields() throws Exception {
            // Credential Constr should have exactly 1 inner field.
            ConstrPlutusData badCred = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(CRED_TRANSFER_HEX)),
                    BytesPlutusData.of(hex("cafe")));
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    badCred,
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsThirdPartyLogicScriptAsPlainBytes() throws Exception {
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    BytesPlutusData.of(hex(CRED_THIRD_PARTY_HEX)),  // not wrapped
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsThirdPartyLogicScriptWithWrongByteLength() throws Exception {
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex("aabbccddeeff00112233")),  // 10 bytes
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsMintingLogicScriptWithWrongByteLength() throws Exception {
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex("aabbccddeeff00112233")),  // 10 bytes
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsUnfrackingLogicScriptAsPlainBytes() throws Exception {
            String datum = serialize(node(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(hex(CRED_UNFRACKING_HEX)),  // not wrapped
                    EMPTY_BYTES));

            assertThat(parser.parse(datum)).isEmpty();
        }
    }

    // ----- global_state_cs field invariants --------------------------------------------

    @Nested
    @DisplayName("global_state_cs field invariants")
    class GlobalStateInvariants {

        @Test
        void acceptsEmptyGlobalState() throws Exception {
            // Already covered by parsesNodeWithAbsentGlobalState — duplicated here for
            // symmetry with the rejection cases in this class.
            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result =
                    parser.parse(buildDatum(hex(POLICY_A_HEX), hex(POLICY_B_HEX), EMPTY_BYTES));

            assertThat(result).isPresent();
            assertThat(result.get().globalStatePolicyId()).isNull();
        }

        @Test
        void acceptsFullLengthGlobalState() throws Exception {
            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result =
                    parser.parse(buildDatum(hex(POLICY_A_HEX), hex(POLICY_B_HEX), hex(GLOBAL_STATE_POLICY_HEX)));

            assertThat(result).isPresent();
            assertThat(result.get().globalStatePolicyId()).isEqualTo(GLOBAL_STATE_POLICY_HEX);
        }

        @Test
        void rejectsGlobalStateWithWrongLength() throws Exception {
            // 10 bytes — neither 0 (absent) nor 28 (real currency symbol).
            assertThat(parser.parse(buildDatum(hex(POLICY_A_HEX), hex(POLICY_B_HEX), hex("aabbccddeeff00112233"))))
                    .isEmpty();
        }

        @Test
        void rejectsGlobalStateAsConstr() throws Exception {
            // Field 6 must be a ByteString, not a Constr.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_MINTING_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(CRED_UNFRACKING_HEX)),
                    vkeyCred(hex(GLOBAL_STATE_POLICY_HEX)));

            assertThat(parser.parse(serialize(node))).isEmpty();
        }
    }

    // ----- Malformed inputs ------------------------------------------------------------

    @Nested
    @DisplayName("Malformed inputs")
    class MalformedInputs {

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
        void returnsEmptyForArbitraryCbor() throws Exception {
            // Valid CBOR but not a ConstrPlutusData — an integer literal.
            String datum = serialize(BigIntPlutusData.of(BigInteger.valueOf(42)));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsHexExceedingMaxSize() {
            // 4097 hex chars — over the 4096-char cap. Uses valid hex characters to prove
            // the rejection is from the size cap, not from HexUtil failing on invalid input.
            // This defends against library-layer DoS (deeply nested CBOR / pre-allocation bomb)
            // by refusing to feed an oversized payload into PlutusData.deserialize at all.
            String tooLarge = "a".repeat(4098); // even length so it's still decodable as hex

            assertThat(parser.parse(tooLarge)).isEmpty();
        }
    }

    // ----- Sentinel detection ----------------------------------------------------------

    @Nested
    @DisplayName("ParsedRegistryNode.isHeadSentinel")
    class SentinelDetection {

        @Test
        void returnsTrueWhenKeyIsEmpty() throws Exception {
            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result =
                    parser.parse(buildDatum(EMPTY_BYTES, hex(POLICY_A_HEX), EMPTY_BYTES));

            assertThat(result).isPresent();
            assertThat(result.get().isHeadSentinel()).isTrue();
        }

        @Test
        void returnsFalseForRealPolicy() throws Exception {
            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result =
                    parser.parse(buildDatum(hex(POLICY_A_HEX), hex(POLICY_B_HEX), EMPTY_BYTES));

            assertThat(result).isPresent();
            assertThat(result.get().isHeadSentinel()).isFalse();
        }

        @Test
        void returnsFalseForTailSentinelNode() throws Exception {
            // A materialized tail sentinel node has a non-empty key (e.g. 32 bytes of 0xFF).
            // It is not the HEAD sentinel. next == key is a self-loop / terminator — allowed by the parser.
            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result =
                    parser.parse(buildDatum(TAIL_SENTINEL_32B, TAIL_SENTINEL_32B, EMPTY_BYTES));

            assertThat(result).isPresent();
            assertThat(result.get().isHeadSentinel()).isFalse();
        }
    }

    // ----- Helpers ---------------------------------------------------------------------

    /**
     * Builds a 7-field RegistryNode (Constr 0):
     * {@code [key, next, minting, transfer, third_party, unfracking, global_state_cs]}.
     */
    private static ConstrPlutusData node(byte[] key,
                                         byte[] next,
                                         PlutusData mintingLogicScript,
                                         PlutusData transferLogicScript,
                                         PlutusData thirdPartyLogicScript,
                                         PlutusData unfrackingLogicScript,
                                         byte[] globalStateCs) {
        return ConstrPlutusData.of(0,
                BytesPlutusData.of(key),
                BytesPlutusData.of(next),
                mintingLogicScript,
                transferLogicScript,
                thirdPartyLogicScript,
                unfrackingLogicScript,
                BytesPlutusData.of(globalStateCs));
    }

    /** A well-formed node with the four distinct VerificationKey credentials; varies key, next and global state. */
    private static String buildDatum(byte[] key, byte[] next, byte[] globalStateCs) throws Exception {
        return serialize(node(key,
                next,
                vkeyCred(hex(CRED_MINTING_HEX)),
                vkeyCred(hex(CRED_TRANSFER_HEX)),
                vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                vkeyCred(hex(CRED_UNFRACKING_HEX)),
                globalStateCs));
    }

    /** Wraps a hash in an Aiken {@code Credential.VerificationKey} Constr (alternative 0). */
    private static PlutusData vkeyCred(byte[] hash) {
        return ConstrPlutusData.of(0, BytesPlutusData.of(hash));
    }

    /** Wraps a hash in an Aiken {@code Credential.Script} Constr (alternative 1). */
    private static PlutusData scriptCred(byte[] hash) {
        return ConstrPlutusData.of(1, BytesPlutusData.of(hash));
    }

    private static String serialize(PlutusData data) throws Exception {
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(data.serialize()));
    }

    private static byte[] hex(String s) {
        return HexUtil.decodeHexString(s);
    }
}
