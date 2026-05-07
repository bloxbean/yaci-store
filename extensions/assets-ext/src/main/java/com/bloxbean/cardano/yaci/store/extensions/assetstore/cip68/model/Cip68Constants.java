package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

public final class Cip68Constants {

    // Asset name prefixes (hex-encoded CIP-68 labels). The reference NFT (label 100)
    // is the metadata-holding token; user-facing tokens use one of the other prefixes
    // depending on type. See https://cips.cardano.org/cip/CIP-68/.
    public static final String REFERENCE_TOKEN_PREFIX     = "000643b0";  // label 100 — reference NFT (metadata holder)
    public static final String NFT_TOKEN_PREFIX           = "000de140";  // label 222 — non-fungible
    public static final String FUNGIBLE_TOKEN_PREFIX      = "0014df10";  // label 333 — fungible
    public static final String RICH_FUNGIBLE_TOKEN_PREFIX = "001bc280";  // label 444 — rich fungible (RFT)

    // CIP-68 label values written to cip68_metadata.label by Cip68Processor based on
    // cross-output detection of the co-minted user-token prefix.
    public static final int LABEL_NFT = 222;
    public static final int LABEL_FT  = 333;
    public static final int LABEL_RFT = 444;

    private Cip68Constants() {
    }

}
