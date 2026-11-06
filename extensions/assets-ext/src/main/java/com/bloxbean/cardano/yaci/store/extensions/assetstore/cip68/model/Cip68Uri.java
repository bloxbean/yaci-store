package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model;

import java.util.regex.Pattern;

/**
 * The CIP-68 {@code uri}: its scheme must be one of {@code https} (HTTP), {@code ipfs} (IPFS), {@code ar} (Arweave) or
 * {@code data} (on-chain). It is the type of the NFT and RFT {@code image}, of each {@code src} in {@code files} and of
 * the fungible token {@code logo}.
 */
public final class Cip68Uri {

    /** Scheme compared without regard to case, and something must follow the colon. */
    private static final Pattern ALLOWED_SCHEME = Pattern.compile("^(https|ipfs|ar|data):.+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** The allowed schemes, for messages. */
    public static final String ALLOWED_SCHEMES = "https, ipfs, ar, data";

    private Cip68Uri() {
    }

    /** True if the value is a URI with one of the schemes CIP-68 allows. */
    public static boolean hasAllowedScheme(String value) {
        return ALLOWED_SCHEME.matcher(value).matches();
    }
}
