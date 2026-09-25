package com.bloxbean.cardano.yaci.store.extensions.assetstore.util;

/**
 * Accepted range for a token's {@code decimals}, shared by the CIP-26 and CIP-68 write paths.
 * <p>
 * CIP-26 tokens use 0–19 in practice; 255 matches the bound already advertised on
 * {@code DecimalsProperty} and leaves headroom. Anything outside the range is rejected before it
 * reaches the {@code BIGINT} column, so consumers can safely narrow the value to an {@code int}.
 */
public final class TokenDecimals {

    public static final long MIN = 0;
    public static final long MAX = 255;

    /** Human-readable form of the range, for log messages. */
    public static final String RANGE = "[" + MIN + ", " + MAX + "]";

    private TokenDecimals() {
    }

    public static boolean isInRange(long decimals) {
        return decimals >= MIN && decimals <= MAX;
    }

}
