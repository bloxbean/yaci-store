package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

import lombok.extern.slf4j.Slf4j;

/**
 * Record/Utility class to group in one place Asset unit, policy and name manipulation and comparison
 *
 * @param policyId  the 56-character hex policy ID
 * @param assetName the hex-encoded asset name (may be empty for policy-only assets)
 */
@Slf4j
public record AssetType(String policyId, String assetName) {

    private static final int POLICY_ID_LENGTH = 56;
    private static final String LOVELACE = "lovelace";
    private static final AssetType ADA_ASSET = new AssetType("", LOVELACE);

    public String toUnit() {
        return policyId + assetName;
    }

    public static AssetType fromUnit(String unit) {
        if (unit.equalsIgnoreCase(LOVELACE) || unit.isBlank()) {
            return ADA_ASSET;
        }

        String sanitized = unit.replace(".", "");
        return switch (Integer.compare(sanitized.length(), POLICY_ID_LENGTH)) {
            case 1  -> new AssetType(sanitized.substring(0, POLICY_ID_LENGTH), sanitized.substring(POLICY_ID_LENGTH));
            case 0  -> new AssetType(sanitized, "");
            default -> {
                log.warn("Invalid unit '{}': must be at least {} hex characters (28-byte policy id)", unit, POLICY_ID_LENGTH);
                yield new AssetType(sanitized, "");
            }
        };
    }

}
