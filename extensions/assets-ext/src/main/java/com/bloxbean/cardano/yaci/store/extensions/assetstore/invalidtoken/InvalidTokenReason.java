package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken;

/**
 * The stable {@code reason_code}s of {@code invalid_token} (ADR 0007), and the standards that report them. A code is never renamed or
 * reused; a new one is added.
 */
public final class InvalidTokenReason {

    public static final String CIP_26 = "CIP-26";
    public static final String CIP_68 = "CIP-68";
    public static final String CIP_113 = "CIP-113";

    // CIP-26: a registry file that cannot become an entry, or a logo that is not stored
    public static final String FILENAME_SUBJECT_MISMATCH = "filename_subject_mismatch";
    public static final String UNUSABLE_SUBJECT = "unusable_subject";
    public static final String UNPARSEABLE_MAPPING = "unparseable_mapping";
    public static final String MISSING_GIT_HISTORY = "missing_git_history";
    /** A logo far beyond any real size: the entry is stored without it. */
    public static final String LOGO_TOO_LARGE = "logo_too_large";

    // CIP-68: an output whose datum is not stored
    public static final String DATUM_UNDECODABLE = "datum_undecodable";
    public static final String DATUM_TOO_DEEP_TO_DECODE = "datum_too_deep_to_decode";

    /** CIP-68 and CIP-26: something nobody foresaw threw while an item was indexed or checked. The one reason to look into. */
    public static final String PROCESSING_FAILURE = "processing_failure";

    // CIP-113: an output holding a registry NFT that is not stored as a registry node (ADR 0011). A registered policy with
    // nothing minted is not one: it is valid, and there is nothing to drop (ADR 0007)
    /** The datum is not a registry node. */
    public static final String REGISTRY_NODE_UNPARSEABLE = "registry_node_unparseable";
    /** The output is not at the registry's address, or holds the registry NFT of another key than its datum's. */
    public static final String REGISTRY_NODE_SPOOFED = "registry_node_spoofed";

    private InvalidTokenReason() {
    }
}
