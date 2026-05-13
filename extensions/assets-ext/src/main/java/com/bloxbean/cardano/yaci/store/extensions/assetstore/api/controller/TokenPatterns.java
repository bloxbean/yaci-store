package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

/**
 * Regex patterns for {@code jakarta.validation.constraints.Pattern} applied to controller
 * path variables and DTO fields. Centralised so the same constants can be referenced from
 * multiple controllers and tests.
 * <p>
 * Values must be compile-time constants so they can appear in annotation element values.
 */
public final class TokenPatterns {

    /**
     * Subject = policyId (28 bytes / 56 hex chars) + optional assetName (up to 32 bytes / 64 hex chars).
     * Total length: 56..120 hex characters.
     */
    public static final String SUBJECT_REGEX = "^[0-9a-fA-F]{56,120}$";

    /** Policy id — Blake2b-224 hash, exactly 56 hex characters. */
    public static final String POLICY_ID_REGEX = "^[0-9a-fA-F]{56}$";

    /** Asset name — 0..32 bytes, hex-encoded (0..64 characters). */
    public static final String ASSET_NAME_REGEX = "^[0-9a-fA-F]{0,64}$";

    /**
     * CIP-68 "raw" asset name — the hex asset name <em>without</em> the 4-byte reference NFT
     * label prefix. The controller prepends the label before querying, so the raw name can be
     * at most 32 - 4 = 28 bytes (0..56 hex characters).
     */
    public static final String CIP68_RAW_ASSET_NAME_REGEX = "^[0-9a-fA-F]{0,56}$";

    private TokenPatterns() {
    }
}
