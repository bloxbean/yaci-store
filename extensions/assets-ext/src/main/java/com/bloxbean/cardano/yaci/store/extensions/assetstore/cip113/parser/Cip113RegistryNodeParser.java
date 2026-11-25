package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Parses CIP-113 registry node inline datums.
 * <p>
 * The datum is the Aiken {@code RegistryNode} record of the CIP-113 reference implementation
 * (cardano-foundation/cip113-programmable-tokens, {@code lib/registry_node.ak}), as deployed on
 * mainnet, preprod and preview, serialized as
 * {@code Constr 0 [key, next, minting_logic_script, transfer_logic_script, third_party_logic_script,
 * unfracking_logic_script, global_state_cs]}:
 * <ol start="0">
 *   <li>{@code key} — {@code ByteArray}. Empty (head sentinel), a 28-byte policy id of a
 *       registered programmable token, or a tail sentinel of up to 32 bytes.</li>
 *   <li>{@code next} — {@code ByteArray}. Non-empty pointer to the next node's {@code key}
 *       in the sorted linked list.</li>
 *   <li>{@code minting_logic_script} — optional {@code Credential}. See "Absent credential
 *       encoding" below.</li>
 *   <li>{@code transfer_logic_script} — optional {@code Credential}. Same shape.</li>
 *   <li>{@code third_party_logic_script} — optional {@code Credential}. Same shape. Stored as
 *       {@code third_party_transfer_logic_script}.</li>
 *   <li>{@code unfracking_logic_script} — optional {@code Credential}. Same shape. Absent
 *       ({@code empty_vkey} on-chain) means unfracking is forbidden for this policy.</li>
 *   <li>{@code global_state_cs} — {@code ByteArray}. Empty bytes mean "no global-state NFT";
 *       28 bytes are a real currency symbol.</li>
 * </ol>
 *
 * <h2>Absent credential encoding</h2>
 * Although the Aiken type signature declares the four credential fields as non-optional
 * {@code Credential}, real CIP-113 registry datums in the wild encode "no credential" using
 * one of these conventions:
 * <ul>
 *   <li>A plain {@code BytesPlutusData(h'')} where a {@code Credential} Constr would normally sit.</li>
 *   <li>A {@code Credential} Constr whose inner byte string is empty.</li>
 * </ul>
 * Both are normalised to {@code null} in the parsed output. Any other deviation from the
 * expected shape or byte-length is rejected with a warning and {@link Optional#empty()}.
 *
 * <h2>Byzantine safety</h2>
 * Every invariant below is enforced at parser level so that malformed or hostile datums
 * cannot reach the DB layer and cause {@code VARCHAR} constraint violations (which would
 * wedge the event-listener transaction on retry).
 *
 * <h2>Non-enforced invariant</h2>
 * The sort invariant {@code key < next} is NOT checked here because materialized tail-sentinel
 * nodes in some aiken-linked-list implementations legitimately violate it. The on-chain
 * minting policy is the source of truth for that invariant.
 */
@Component
@Slf4j
public class Cip113RegistryNodeParser {

    /** Exact number of fields in a well-formed {@code RegistryNode} datum. */
    private static final int EXPECTED_FIELD_COUNT = 7;

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
     * Real policy ids are 28 bytes (head sentinel is 0 bytes). The aiken-linked-list
     * implementation in use on preprod materializes a tail sentinel of 30 bytes of
     * {@code 0xFF}; other library versions may go up to 32 bytes. 32 bytes is therefore
     * the tightest defensible upper bound — matches the DB column length
     * ({@code VARCHAR(64)} = 64 hex chars = 32 bytes).
     */
    private static final int MAX_KEY_BYTE_LEN = 32;

    /**
     * Hard cap on the size of the serialized inline datum <em>before</em> it reaches the
     * recursive CBOR decoder. A well-formed CIP-113 {@code RegistryNode} encodes to roughly
     * 200–400 bytes (≤ 800 hex chars) in the worst realistic case. 4096 hex chars (2048
     * bytes) gives ~10× margin while tightly bounding the input fed to
     * {@code PlutusData.deserialize} — prevents the library-layer DoS classes (deeply
     * nested Constr → {@link StackOverflowError}; CBOR pre-allocation bomb →
     * {@link OutOfMemoryError}) from ever reaching the recursive decoder at all.
     */
    private static final int MAX_DATUM_HEX_LEN = 4096;

    /**
     * Parsed registry node fields. Byte-string fields are lowercase hex. {@code key} and
     * {@code next} are always non-null; the five optional script/policy fields are null
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
                                     @Nullable String mintingLogicScript,
                                     @Nullable String transferLogicScript,
                                     @Nullable String thirdPartyTransferLogicScript,
                                     @Nullable String unfrackingLogicScript,
                                     @Nullable String globalStatePolicyId) {

        /**
         * @return true if this is the head sentinel of the sorted linked list (empty {@code key}).
         */
        public boolean isHeadSentinel() {
            return key.isEmpty();
        }
    }

    /**
     * The outcome of reading a registry node datum: the node, or why it is rejected (the reason is recorded in
     * {@code invalid_token}, ADR 0007).
     */
    public record ParseResult(@Nullable ParsedRegistryNode node, @Nullable String rejection) {

        static ParseResult parsed(ParsedRegistryNode node) {
            return new ParseResult(node, null);
        }

        static ParseResult rejected(String rejection) {
            return new ParseResult(null, rejection);
        }
    }

    public Optional<ParsedRegistryNode> parse(String inlineDatum) {
        return Optional.ofNullable(parseDetailed(inlineDatum).node());
    }

    /** Like {@link #parse(String)}, but says why a datum is rejected; each rejection is also logged at WARN. */
    public ParseResult parseDetailed(@Nullable String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return ParseResult.rejected("the registry node output has no inline datum");
        }

        // Hard size cap BEFORE we touch HexUtil or the recursive CBOR decoder.
        // This is the only parser-layer defence against library-layer DoS (deep nesting
        // or pre-allocation bombs) — those attacks execute inside PlutusData.deserialize
        // and manifest as Error instances (not RuntimeException), which our catch clause
        // deliberately does not swallow. Bounding the input at 4096 hex chars keeps the
        // decoder's working set small enough that neither class of attack is realistic.
        if (inlineDatum.length() > MAX_DATUM_HEX_LEN) {
            return rejected("CIP-113 registry node: inline datum too large ({} hex chars, max {})",
                    inlineDatum.length(), MAX_DATUM_HEX_LEN);
        }

        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            // Invariant #1: root must be a Constr with alternative 0.
            if (!(plutusData instanceof ConstrPlutusData constr)) {
                return rejected("CIP-113 registry node: expected root ConstrPlutusData, got {}",
                        plutusData.getClass().getSimpleName());
            }
            if (constr.getAlternative() != REGISTRY_NODE_CONSTR_ALTERNATIVE) {
                return rejected("CIP-113 registry node: expected Constr alternative {}, got {}",
                        REGISTRY_NODE_CONSTR_ALTERNATIVE, constr.getAlternative());
            }

            // Invariant #2: exactly 7 fields.
            List<PlutusData> fields = constr.getData().getPlutusDataList();
            if (fields.size() != EXPECTED_FIELD_COUNT) {
                return rejected("CIP-113 registry node: expected {} fields, got {}",
                        EXPECTED_FIELD_COUNT, fields.size());
            }

            // Invariant #3: key — ByteString, 0 to 32 bytes (head sentinel, real policy, or tail sentinel).
            byte[] keyBytes = extractByteArray(fields.get(0));
            if (keyBytes == null || keyBytes.length > MAX_KEY_BYTE_LEN) {
                return rejected("CIP-113 registry node: invalid 'key' field (expected ByteString of length 0..{} bytes)",
                        MAX_KEY_BYTE_LEN);
            }

            // Invariant #4: next — ByteString, 1 to 32 bytes (must be non-empty).
            byte[] nextBytes = extractByteArray(fields.get(1));
            if (nextBytes == null || nextBytes.length == 0 || nextBytes.length > MAX_KEY_BYTE_LEN) {
                return rejected("CIP-113 registry node: invalid 'next' field (expected ByteString of length 1..{} bytes)",
                        MAX_KEY_BYTE_LEN);
            }

            // Invariants #5–#8: the four logic scripts — optional Credentials (null when empty).
            String mintingLogicScript = extractOptionalCredentialHash(fields.get(2), "minting_logic_script");
            String transferLogicScript = extractOptionalCredentialHash(fields.get(3), "transfer_logic_script");
            String thirdPartyTransferLogicScript = extractOptionalCredentialHash(
                    fields.get(4), "third_party_logic_script");
            String unfrackingLogicScript = extractOptionalCredentialHash(fields.get(5), "unfracking_logic_script");

            // Invariant #9: global_state_cs — ByteString, 0 bytes (null) or exactly 28 bytes.
            String globalStatePolicyId = extractOptionalGlobalStateCs(fields.get(6));

            return ParseResult.parsed(new ParsedRegistryNode(
                    HexUtil.encodeHexString(keyBytes),
                    HexUtil.encodeHexString(nextBytes),
                    mintingLogicScript,
                    transferLogicScript,
                    thirdPartyTransferLogicScript,
                    unfrackingLogicScript,
                    globalStatePolicyId));

        } catch (InvalidDatumException e) {
            return rejected("CIP-113 registry node rejected: {}", e.getMessage());
        } catch (CborDeserializationException | RuntimeException e) {
            // Deliberately narrower than catching every exception, so that an Error such as an
            // OutOfMemoryError still propagates. This covers malformed CBOR rejected by the
            // PlutusData decoder and bad hex input rejected by HexUtil.
            return rejected("Failed to parse CIP-113 registry node datum: {}", e.getMessage());
        }
    }

    /** Logs a rejected datum at WARN, with the message formatted as in the log, and returns it as the rejection. */
    private static ParseResult rejected(String format, Object... arguments) {
        String message = MessageFormatter.arrayFormat(format, arguments).getMessage();
        log.warn(message);
        return ParseResult.rejected(message);
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
     * Extracts an optional credential hash from one of the four logic-script fields.
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
