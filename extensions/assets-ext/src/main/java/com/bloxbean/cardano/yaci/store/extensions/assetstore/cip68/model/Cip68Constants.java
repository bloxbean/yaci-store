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

    /**
     * Derives the CIP-68 user token label from a reference NFT asset name.
     * Currently only fungible tokens (label 333) are supported.
     * When NFT support is added, this should inspect the co-minted user token
     * to determine if it's FT (333), NFT (222), or RFT (444).
     *
     * @param referenceNftAssetName the asset name of the reference NFT (with 000643b0 prefix).
     *                              Currently unused — reserved for future NFT/RFT label derivation.
     * @return the label value (currently always 333)
     */
    @SuppressWarnings("java:S1172") // parameter will be used when NFT support is added
    public static int labelFromReferenceNft(String referenceNftAssetName) {
        // TODO: derive actual label when NFT/RFT support is added
        // For now, all reference NFTs we index are for fungible tokens
        return LABEL_FT;
    }

    private Cip68Constants() {
    }

}
