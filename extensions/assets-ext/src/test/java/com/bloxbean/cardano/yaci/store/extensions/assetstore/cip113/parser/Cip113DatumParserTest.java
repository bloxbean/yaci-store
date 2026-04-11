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
class Cip113DatumParserTest {

    // All hash-shaped values are 56 hex chars = 28 bytes (Blake2b-224 / policy id).
    private static final String POLICY_A_HEX = "0befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d1";
    private static final String POLICY_B_HEX = "1befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d2";
    private static final String CRED_TRANSFER_HEX = "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102";
    private static final String CRED_THIRD_PARTY_HEX = "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103";
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
        void parsesFullRegistryNodeWithVKeyCredentials() throws Exception {
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    hex(GLOBAL_STATE_POLICY_HEX));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo(POLICY_A_HEX);
            assertThat(result.get().next()).isEqualTo(POLICY_B_HEX);
            assertThat(result.get().transferLogicScript()).isEqualTo(CRED_TRANSFER_HEX);
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo(CRED_THIRD_PARTY_HEX);
            assertThat(result.get().globalStatePolicyId()).isEqualTo(GLOBAL_STATE_POLICY_HEX);
        }

        @Test
        void parsesNodeWithScriptCredential() throws Exception {
            // Credential alternative 1 = Script (vs 0 = VerificationKey). Both are valid.
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    scriptCred(hex(CRED_TRANSFER_HEX)),
                    scriptCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            // Note: the VKey vs Script variant is intentionally discarded — both surface as the same hex.
            assertThat(result.get().transferLogicScript()).isEqualTo(CRED_TRANSFER_HEX);
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo(CRED_THIRD_PARTY_HEX);
        }

        @Test
        void parsesHeadSentinelNode() throws Exception {
            // key = empty (head sentinel), next points to first real node.
            String datum = buildDatum(
                    EMPTY_BYTES,
                    hex(POLICY_A_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEmpty();
            assertThat(result.get().next()).isEqualTo(POLICY_A_HEX);
            assertThat(result.get().globalStatePolicyId()).isNull();
        }

        @Test
        void parsesNodeWithAbsentGlobalState() throws Exception {
            // global_state_cs = empty bytes → semantically absent → null in the parsed output.
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().globalStatePolicyId()).isNull();
        }

        @Test
        void parsesNodeWithMaxLengthNextSentinel() throws Exception {
            // A materialized tail sentinel node: next points to the 32-byte 0xFF sentinel.
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    TAIL_SENTINEL_32B,
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().next()).isEqualTo("ff".repeat(32));
        }

        @Test
        void parsesNodeWithAbsentTransferLogicScriptAsPlainEmptyBytes() throws Exception {
            // Real-world tolerance: plain BytesPlutusData(h'') in place of a Credential Constr
            // means "no transfer_logic_script".
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isNull();
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo(CRED_THIRD_PARTY_HEX);
        }

        @Test
        void parsesNodeWithAbsentTransferLogicScriptAsWrappedEmptyBytes() throws Exception {
            // Also tolerate a Credential Constr whose inner ByteString is empty.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(EMPTY_BYTES),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isNull();
        }

        @Test
        void parsesNodeWithAbsentThirdPartyTransferLogicScript() throws Exception {
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isEqualTo(CRED_TRANSFER_HEX);
            assertThat(result.get().thirdPartyTransferLogicScript()).isNull();
        }

        @Test
        void parsesNodeWithAllThreeOptionalFieldsAbsent() throws Exception {
            // Demonstrates that all three optional fields (both credentials + global_state_cs)
            // can be simultaneously null — only 'key' and 'next' are strictly required.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);

            assertThat(result).isPresent();
            assertThat(result.get().key()).isEqualTo(POLICY_A_HEX);
            assertThat(result.get().next()).isEqualTo(POLICY_B_HEX);
            assertThat(result.get().transferLogicScript()).isNull();
            assertThat(result.get().thirdPartyTransferLogicScript()).isNull();
            assertThat(result.get().globalStatePolicyId()).isNull();
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
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsFewerThanFiveFields() throws Exception {
            // 4 fields — missing global_state_cs. Spec requires all 5.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsMoreThanFiveFields() throws Exception {
            // 6 fields — an extra unknown trailing field.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES),
                    BytesPlutusData.of(hex("cafe")));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
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
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsKeyExceedingMaxLength() throws Exception {
            // 33-byte key — exceeds the 32-byte cap (max sentinel convention).
            byte[] tooLong = new byte[33];
            java.util.Arrays.fill(tooLong, (byte) 0xFF);

            String datum = buildDatum(
                    tooLong,
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsEmptyNext() throws Exception {
            // next must be non-empty — an empty next would violate key < next for any key.
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    EMPTY_BYTES,
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsNextAsNonByteString() throws Exception {
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    vkeyCred(hex(POLICY_B_HEX)),  // wrong type for field 1
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsNextExceedingMaxLength() throws Exception {
            byte[] tooLong = new byte[33];
            java.util.Arrays.fill(tooLong, (byte) 0xFF);

            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    tooLong,
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            assertThat(parser.parse(datum)).isEmpty();
        }
    }

    // ----- Credential field invariants ------------------------------------------------

    @Nested
    @DisplayName("Credential field invariants")
    class CredentialInvariants {

        @Test
        void rejectsTransferLogicScriptAsPlainBytes() throws Exception {
            // Field 2 is a plain BytesPlutusData instead of Constr — the old parser's
            // off-spec tolerance incorrectly returned null here; the new parser rejects.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    BytesPlutusData.of(hex(CRED_TRANSFER_HEX)),  // not wrapped in Credential Constr
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsTransferLogicScriptWithWrongByteLength() throws Exception {
            // Credential wraps 10 bytes instead of 28.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex("aabbccddeeff00112233")),  // 10 bytes
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsTransferLogicScriptWithInvalidAlternative() throws Exception {
            // Aiken Credential alternatives are {0=VerificationKey, 1=Script}. Anything else is invalid.
            ConstrPlutusData badCred = ConstrPlutusData.of(2, BytesPlutusData.of(hex(CRED_TRANSFER_HEX)));

            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    badCred,
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsTransferLogicScriptWithMultipleInnerFields() throws Exception {
            // Credential Constr should have exactly 1 inner field.
            ConstrPlutusData badCred = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(CRED_TRANSFER_HEX)),
                    BytesPlutusData.of(hex("cafe")));

            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    badCred,
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsThirdPartyTransferLogicScriptAsPlainBytes() throws Exception {
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    BytesPlutusData.of(hex(CRED_THIRD_PARTY_HEX)),  // not wrapped
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsThirdPartyTransferLogicScriptWithWrongByteLength() throws Exception {
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex("aabbccddeeff00112233")),  // 10 bytes
                    BytesPlutusData.of(EMPTY_BYTES));
            String datum = serialize(node);

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
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);
            assertThat(result).isPresent();
            assertThat(result.get().globalStatePolicyId()).isNull();
        }

        @Test
        void acceptsFullLengthGlobalState() throws Exception {
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    hex(GLOBAL_STATE_POLICY_HEX));

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);
            assertThat(result).isPresent();
            assertThat(result.get().globalStatePolicyId()).isEqualTo(GLOBAL_STATE_POLICY_HEX);
        }

        @Test
        void rejectsGlobalStateWithWrongLength() throws Exception {
            // 10 bytes — neither 0 (absent) nor 28 (real currency symbol).
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    hex("aabbccddeeff00112233"));

            assertThat(parser.parse(datum)).isEmpty();
        }

        @Test
        void rejectsGlobalStateAsConstr() throws Exception {
            // Field 4 must be a ByteString, not a Constr.
            ConstrPlutusData node = ConstrPlutusData.of(0,
                    BytesPlutusData.of(hex(POLICY_A_HEX)),
                    BytesPlutusData.of(hex(POLICY_B_HEX)),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    vkeyCred(hex(GLOBAL_STATE_POLICY_HEX)));
            String datum = serialize(node);

            assertThat(parser.parse(datum)).isEmpty();
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
    }

    // ----- Sentinel detection ----------------------------------------------------------

    @Nested
    @DisplayName("ParsedRegistryNode.isHeadSentinel")
    class SentinelDetection {

        @Test
        void returnsTrueWhenKeyIsEmpty() throws Exception {
            String datum = buildDatum(
                    EMPTY_BYTES,
                    hex(POLICY_A_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);
            assertThat(result).isPresent();
            assertThat(result.get().isHeadSentinel()).isTrue();
        }

        @Test
        void returnsFalseForRealPolicy() throws Exception {
            String datum = buildDatum(
                    hex(POLICY_A_HEX),
                    hex(POLICY_B_HEX),
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);
            assertThat(result).isPresent();
            assertThat(result.get().isHeadSentinel()).isFalse();
        }

        @Test
        void returnsFalseForTailSentinelNode() throws Exception {
            // A materialized tail sentinel node has a non-empty key (e.g. 32 bytes of 0xFF).
            // It is not the HEAD sentinel.
            String datum = buildDatum(
                    TAIL_SENTINEL_32B,
                    TAIL_SENTINEL_32B,  // self-loop / terminator — allowed by the parser
                    vkeyCred(hex(CRED_TRANSFER_HEX)),
                    vkeyCred(hex(CRED_THIRD_PARTY_HEX)),
                    EMPTY_BYTES);

            Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(datum);
            assertThat(result).isPresent();
            assertThat(result.get().isHeadSentinel()).isFalse();
        }
    }

    // ----- Helpers ---------------------------------------------------------------------

    /** Builds a well-formed 5-field RegistryNode datum (Constr 0) and returns its hex serialization. */
    private static String buildDatum(byte[] key,
                                     byte[] next,
                                     PlutusData transferLogicScript,
                                     PlutusData thirdPartyTransferLogicScript,
                                     byte[] globalStateCs) throws Exception {
        ConstrPlutusData node = ConstrPlutusData.of(0,
                BytesPlutusData.of(key),
                BytesPlutusData.of(next),
                transferLogicScript,
                thirdPartyTransferLogicScript,
                BytesPlutusData.of(globalStateCs));
        return serialize(node);
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
