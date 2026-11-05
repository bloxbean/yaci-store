package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

/**
 * What reading an inline datum gave: a datum that was read, a datum that is not a CIP-68 datum (ignored, since reference NFT outputs can carry
 * any datum), or a datum that is dropped as unusable or an attack, with the reason (recorded in {@code invalid_token}, ADR 0007).
 */
public sealed interface ParseOutcome {

    record Parsed(ParsedCip68Datum datum) implements ParseOutcome {
    }

    record NotCip68() implements ParseOutcome {
    }

    /** {@code reasonCode} is one of {@code InvalidTokenReason}; {@code message} is for people. */
    record Dropped(String reasonCode, String message) implements ParseOutcome {
    }
}
