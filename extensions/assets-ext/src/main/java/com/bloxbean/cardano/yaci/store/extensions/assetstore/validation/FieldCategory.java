package com.bloxbean.cardano.yaci.store.extensions.assetstore.validation;

/**
 * Whether the CIP requires the field ({@link #MANDATORY}) or allows leaving it out ({@link #OPTIONAL}).
 * A token with a {@code MANDATORY} error is still indexed and served; a wallet that wants to be strict
 * can reject it.
 */
public enum FieldCategory {
    MANDATORY,
    OPTIONAL
}
