package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

/**
 * Why a parsed CIP-68 datum is not indexed: a machine-readable reason (also the {@code reason} tag of the skipped
 * counter) and the message the warning shows.
 */
public record DatumRejection(Reason reason, String message) {

    public enum Reason {
        NO_NAME,
        NO_DESCRIPTION,
        NO_IMAGE,
        BAD_IMAGE_SCHEME;

        /** The value of the {@code reason} tag, for example {@code no_image}. */
        public String tag() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
