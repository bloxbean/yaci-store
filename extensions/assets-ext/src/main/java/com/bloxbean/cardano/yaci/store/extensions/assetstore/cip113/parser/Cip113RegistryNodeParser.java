package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Parses CIP-113 registry node inline datums.
 * <p>
 * The datum is an Aiken {@code RegistryNode} record serialized as
 * {@code Constr 0 [key, next, transfer_logic_script, third_party_transfer_logic_script, global_state_cs]}:
 * <ol>
 *   <li>{@code key} — {@code ByteArray}. Empty (head sentinel), a 28-byte policy id of a
 *       registered programmable token, or a tail sentinel of up to 32 bytes.</li>
 *   <li>{@code next} — {@code ByteArray}. Non-empty pointer to the next node's {@code key}
 *       in the sorted linked list.</li>
 *   <li>{@code transfer_logic_script} — optional {@code Credential}. See "Absent credential
 *       encoding" below.</li>
 *   <li>{@code third_party_transfer_logic_script} — optional {@code Credential}. Same shape.</li>
 *   <li>{@code global_state_cs} — {@code ByteArray}. Empty bytes mean "no global-state NFT";
 *       28 bytes are a real currency symbol.</li>
 * </ol>
 *
 * <h3>Absent credential encoding</h3>
 * Although the Aiken type signature declares the two credential fields as non-optional
 * {@code Credential}, real CIP-113 registry datums in the wild encode "no credential" using
 * one of these conventions:
 * <ul>
 *   <li>A plain {@code BytesPlutusData(h'')} where a {@code Credential} Constr would normally sit.</li>
 *   <li>A {@code Credential} Constr whose inner byte string is empty.</li>
 * </ul>
 * Both are normalised to {@code null} in the parsed output. Any other deviation from the
 * expected shape or byte-length is rejected with a warning and {@link Optional#empty()}.
 *
 * <h3>Non-enforced invariant</h3>
 * The sort invariant {@code key < next} is NOT checked here because materialized tail-sentinel
 * nodes in some aiken-linked-list implementations legitimately violate it. The on-chain
 * minting policy is the source of truth for that invariant.
 */
@Component
@Slf4j
public class Cip113RegistryNodeParser {

    /** Exact number of fields in a well-formed {@code RegistryNode} datum. */
    private static final int EXPECTED_FIELD_COUNT = 5;

    /** Aiken compiles {@code RegistryNode{…}} to {@code Constr 0}; no other alternative is valid. */
    private static final long REGISTRY_NODE_CONSTR_ALTERNATIVE = 0L;

    /** Aiken {@code Credential} alternatives: {@code VerificationKey}=0, {@code Script}=1. */
    private static final long CREDENTIAL_VKEY_ALTERNATIVE = 0L;
    private static final long CREDENTIAL_SCRIPT_ALTERNATIVE = 1L;

    /** Blake2b-224 hash length — 28 bytes for policy ids, script hashes, and vkey hashes. */
    private static final int HASH_BYTE_LEN = 28;

    /**
     * Maximum accepted byte length for the {@code key} and {@code next} fields.
     * <p>
     * Real policy ids are 28 bytes (head sentinel is 0 bytes). Some aiken-linked-list
     * implementations materialize a tail sentinel whose key is conventionally 32 bytes
     * (all {@code 0xFF}). 32 bytes is therefore the tightest defensible upper bound —
     * matches the DB column length ({@code VARCHAR(64)} = 64 hex chars = 32 bytes).
     */
    private static final int MAX_KEY_BYTE_LEN = 32;

    /**
     * Parsed registry node fields. Byte-string fields are lowercase hex. {@code key} and
     * {@code next} are always non-null; the three optional script/policy fields are null
     * when the corresponding on-chain field encodes "absent" (empty bytes — see class
     * javadoc for the exact encoding).
     * <p>
     * Note: the Aiken {@code Credential} variant ({@code VerificationKey} vs {@code Script})
     * is intentionally NOT preserved — both hash types are 28 bytes and downstream storage
     * flattens them to a single column. If the distinction becomes load-bearing downstream,
     * add a companion column and a second field here.
     */
    public record ParsedRegistryNode(String key,
                                     String next,
                                     @Nullable String transferLogicScript,
                                     @Nullable String thirdPartyTransferLogicScript,
                                     @Nullable String globalStatePolicyId) {

        /**
         * @return true if this is the head sentinel of the sorted linked list (empty {@code key}).
         */
        public boolean isHeadSentinel() {
            return key.isEmpty();
        }
    }

    public Optional<ParsedRegistryNode> parse(String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return Optional.empty();
        }

        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            // Invariant #1: root must be a Constr with alternative 0.
            if (!(plutusData instanceof ConstrPlutusData constr)) {
                log.warn("CIP-113 registry node: expected root ConstrPlutusData, got {}",
                        plutusData.getClass().getSimpleName());
                return Optional.empty();
            }
            if (constr.getAlternative() != REGISTRY_NODE_CONSTR_ALTERNATIVE) {
                log.warn("CIP-113 registry node: expected Constr alternative {}, got {}",
                        REGISTRY_NODE_CONSTR_ALTERNATIVE, constr.getAlternative());
                return Optional.empty();
            }

            // Invariant #2: exactly 5 fields.
            List<PlutusData> fields = constr.getData().getPlutusDataList();
            if (fields.size() != EXPECTED_FIELD_COUNT) {
                log.warn("CIP-113 registry node: expected {} fields, got {}",
                        EXPECTED_FIELD_COUNT, fields.size());
                return Optional.empty();
            }

            // Invariant #3: key — ByteString, 0 to 32 bytes (head sentinel, real policy, or tail sentinel).
            byte[] keyBytes = extractByteArray(fields.get(0));
            if (keyBytes == null || keyBytes.length > MAX_KEY_BYTE_LEN) {
                log.warn("CIP-113 registry node: invalid 'key' field (expected ByteString of length 0..{} bytes)",
                        MAX_KEY_BYTE_LEN);
                return Optional.empty();
            }

            // Invariant #4: next — ByteString, 1 to 32 bytes (must be non-empty).
            byte[] nextBytes = extractByteArray(fields.get(1));
            if (nextBytes == null || nextBytes.length == 0 || nextBytes.length > MAX_KEY_BYTE_LEN) {
                log.warn("CIP-113 registry node: invalid 'next' field (expected ByteString of length 1..{} bytes)",
                        MAX_KEY_BYTE_LEN);
                return Optional.empty();
            }

            // Invariant #5: transfer_logic_script — optional Credential (null when empty bytes).
            String transferLogicScript = extractOptionalCredentialHash(fields.get(2), "transfer_logic_script");

            // Invariant #6: third_party_transfer_logic_script — optional Credential.
            String thirdPartyTransferLogicScript = extractOptionalCredentialHash(
                    fields.get(3), "third_party_transfer_logic_script");

            // Invariant #7: global_state_cs — ByteString, 0 bytes (null) or exactly 28 bytes.
            String globalStatePolicyId = extractOptionalGlobalStateCs(fields.get(4));

            return Optional.of(new ParsedRegistryNode(
                    HexUtil.encodeHexString(keyBytes),
                    HexUtil.encodeHexString(nextBytes),
                    transferLogicScript,
                    thirdPartyTransferLogicScript,
                    globalStatePolicyId));

        } catch (InvalidDatumException e) {
            log.warn("CIP-113 registry node rejected: {}", e.getMessage());
            return Optional.empty();
        } catch (CborDeserializationException | RuntimeException e) {
            // Narrowed from catch(Exception): we deliberately let Error (OOM, etc.) propagate.
            // Covers CborDeserializationException from PlutusData.deserialize on malformed CBOR,
            // plus IllegalArgumentException from HexUtil on bad hex input.
            log.warn("Failed to parse CIP-113 registry node datum: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the raw bytes if {@code data} is a {@link BytesPlutusData}, or {@code null}
     * if it is any other PlutusData variant. An empty byte array is a valid return value
     * (distinguishes "wrong type" from "empty bytes").
     */
    @Nullable
    private byte[] extractByteArray(PlutusData data) {
        if (data instanceof BytesPlutusData bytes) {
            return bytes.getValue();
        }
        return null;
    }

    /**
     * Extracts an optional credential hash from one of the two script fields.
     * <p>
     * Accepts:
     * <ul>
     *   <li>An Aiken {@code Credential} Constr ({@code VerificationKey} alt 0 or {@code Script}
     *       alt 1) wrapping a 28-byte ByteString → returns the 56-hex-char hash.</li>
     *   <li>{@code BytesPlutusData(h'')} (plain empty bytes, off-spec but convention in the
     *       wild) → returns {@code null}.</li>
     *   <li>A {@code Credential} Constr whose inner ByteString is empty → returns {@code null}.</li>
     * </ul>
     * Throws {@link InvalidDatumException} for any other shape or a wrong inner byte length,
     * which causes the enclosing {@link #parse(String)} to reject the whole datum.
     */
    @Nullable
    private String extractOptionalCredentialHash(PlutusData data, String fieldName) {
        // Off-spec tolerance: plain BytesPlutusData in place of a Credential Constr.
        // Empty bytes are accepted as "absent credential"; non-empty plain bytes are malformed.
        if (data instanceof BytesPlutusData bytes) {
            if (bytes.getValue().length == 0) {
                return null;
            }
            throw new InvalidDatumException("'" + fieldName
                    + "' is a non-empty ByteString (expected Credential Constr or empty bytes)");
        }
        if (!(data instanceof ConstrPlutusData constr)) {
            throw new InvalidDatumException("'" + fieldName
                    + "' must be Credential Constr or empty ByteString, got "
                    + data.getClass().getSimpleName());
        }
        long alt = constr.getAlternative();
        if (alt != CREDENTIAL_VKEY_ALTERNATIVE && alt != CREDENTIAL_SCRIPT_ALTERNATIVE) {
            throw new InvalidDatumException("'" + fieldName
                    + "' Credential has invalid alternative " + alt + " (expected 0 or 1)");
        }
        List<PlutusData> inner = constr.getData().getPlutusDataList();
        if (inner.size() != 1) {
            throw new InvalidDatumException("'" + fieldName
                    + "' Credential must have exactly 1 inner field, got " + inner.size());
        }
        byte[] hash = extractByteArray(inner.get(0));
        if (hash == null) {
            throw new InvalidDatumException("'" + fieldName + "' Credential inner must be a ByteString");
        }
        // Also tolerate an explicitly wrapped empty-hash as "absent credential".
        if (hash.length == 0) {
            return null;
        }
        if (hash.length != HASH_BYTE_LEN) {
            throw new InvalidDatumException("'" + fieldName
                    + "' Credential inner hash must be " + HASH_BYTE_LEN + " bytes, got " + hash.length);
        }
        return HexUtil.encodeHexString(hash);
    }

    /**
     * Extracts the optional {@code global_state_cs} currency symbol. Empty bytes → null;
     * exactly 28 bytes → hex; anything else throws {@link InvalidDatumException}.
     */
    @Nullable
    private String extractOptionalGlobalStateCs(PlutusData data) {
        byte[] bytes = extractByteArray(data);
        if (bytes == null) {
            throw new InvalidDatumException(
                    "'global_state_cs' must be a ByteString, got " + data.getClass().getSimpleName());
        }
        if (bytes.length == 0) {
            return null;
        }
        if (bytes.length != HASH_BYTE_LEN) {
            throw new InvalidDatumException("'global_state_cs' must be 0 or " + HASH_BYTE_LEN
                    + " bytes, got " + bytes.length);
        }
        return HexUtil.encodeHexString(bytes);
    }

    /** Thrown from helpers to bail out of {@link #parse(String)} with a descriptive reason. */
    private static final class InvalidDatumException extends RuntimeException {
        InvalidDatumException(String message) {
            super(message);
        }
    }
}
