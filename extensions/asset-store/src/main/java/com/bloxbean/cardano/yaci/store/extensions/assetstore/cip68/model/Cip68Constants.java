package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

public final class Cip68Constants {

    // Asset name prefixes (hex-encoded CIP-68 labels)
    public static final String REFERENCE_TOKEN_PREFIX = "000643b0";  // label 100
    public static final String NFT_TOKEN_PREFIX = "000de140";        // label 222
    public static final String FUNGIBLE_TOKEN_PREFIX = "0014df10";   // label 333
    public static final String RFT_TOKEN_PREFIX = "001bc280";        // label 444

    // CIP-68 label values
    public static final int LABEL_REFERENCE_NFT = 100;
    public static final int LABEL_NFT = 222;
    public static final int LABEL_FT = 333;
    public static final int LABEL_RFT = 444;

    private Cip68Constants() {
    }

}
