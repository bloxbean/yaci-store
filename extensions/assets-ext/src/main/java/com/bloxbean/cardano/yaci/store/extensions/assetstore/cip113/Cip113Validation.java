package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationCode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;

/**
 * What is reported about a programmable token. A CIP-113 registry node says only that a policy is programmable: the
 * token's name, ticker, decimals and logo come from CIP-26 or CIP-68, its <em>companion</em> metadata. A token of a
 * registered policy without any is still served, with this error, as a token that breaks CIP-26 or CIP-68 is (ADR 0006,
 * ADR 0007).
 */
public final class Cip113Validation {

    /** The field of the error: the CIP-26 or CIP-68 metadata of the programmable token. */
    public static final String COMPANION_METADATA = "companion_metadata";

    private Cip113Validation() {
    }

    /**
     * The error of a programmable token that has no CIP-26 or CIP-68 metadata. It is {@code MANDATORY}: the token is
     * served under {@code ?validation=ALL} and left out by {@code MANDATORY_ONLY} and {@code STRICT}.
     */
    public static ValidationError missingCompanion() {
        return ValidationError.mandatory(ValidationError.CIP_113, null, COMPANION_METADATA, ValidationCode.MISSING_VALUE,
                "the policy is registered in the CIP-113 registry, but the token has no CIP-26 or CIP-68 metadata: "
                        + "no name or description (mandatory in both), and no ticker, decimals or logo");
    }
}
