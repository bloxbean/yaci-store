package com.bloxbean.cardano.yaci.store.submit.quicktx.signing;

import java.util.Locale;

/**
 * Utility class for signer scope normalization.
 */
public final class SignerScopeUtil {

    private SignerScopeUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Normalize a scope string by trimming whitespace and converting to lowercase.
     *
     * @param scope the scope string to normalize
     * @return the normalized scope string, or empty string if null
     */
    public static String normalize(String scope) {
        if (scope == null) {
            return "";
        }
        return scope.trim().toLowerCase(Locale.ROOT);
    }
}
