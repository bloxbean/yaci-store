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

    private static final String LOVELACE = "lovelace";

    private static final AssetType ADA_ASSET = new AssetType("", LOVELACE);

    public String toUnit() {
        return policyId + assetName;
    }

    public static AssetType fromUnit(String unit) {
        if (unit.equalsIgnoreCase(LOVELACE) || unit.trim().isEmpty()) {
            return ADA_ASSET;
        }

        String sanitizedUnit = unit.replace(".", "");
        if (sanitizedUnit.length() > 56) {
            return new AssetType(sanitizedUnit.substring(0, 56), sanitizedUnit.substring(56));
        } else if (sanitizedUnit.length() == 56) {
            return new AssetType(sanitizedUnit, "");
        } else {
            log.warn("Invalid unit '{}': must be at least 56 hex characters (28-byte policy id)", unit);
            return new AssetType(sanitizedUnit, "");
        }
    }

}
