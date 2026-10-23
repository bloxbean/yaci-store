package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken;

/**
 * The stable {@code reason_code}s of {@code invalid_token} (ADR 0007), and the standards that report them. A code is never renamed or
 * reused; a new one is added.
 */
public final class InvalidTokenReason {

    public static final String CIP_26 = "CIP-26";
    public static final String CIP_68 = "CIP-68";
    public static final String CIP_113 = "CIP-113";

    // CIP-26: a registry file that cannot become an entry
    public static final String FILENAME_SUBJECT_MISMATCH = "filename_subject_mismatch";
    public static final String UNUSABLE_SUBJECT = "unusable_subject";
    public static final String UNPARSEABLE_MAPPING = "unparseable_mapping";
    public static final String MISSING_GIT_HISTORY = "missing_git_history";

    // CIP-68: an output whose datum is not stored
    public static final String DATUM_UNDECODABLE = "datum_undecodable";
    public static final String DATUM_TOO_DEEP_TO_DECODE = "datum_too_deep_to_decode";
    public static final String PROCESSING_FAILURE = "processing_failure";

    // CIP-113, reserved: documented in ADR 0007, written when CIP-113 is indexed again
    public static final String JAIL_WITHOUT_TOKEN = "jail_without_token";
    public static final String UNREGISTERED_POLICY_IN_JAIL = "unregistered_policy_in_jail";
    public static final String REGISTRY_NODE_UNPARSEABLE = "registry_node_unparseable";
    public static final String REGISTRY_NODE_SPOOFED = "registry_node_spoofed";

    private InvalidTokenReason() {
    }
}
