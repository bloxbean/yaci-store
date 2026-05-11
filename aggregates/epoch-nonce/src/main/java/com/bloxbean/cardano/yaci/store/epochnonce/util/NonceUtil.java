package com.bloxbean.cardano.yaci.store.epochnonce.util;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;

/**
 * Utility methods for Ouroboros Praos epoch nonce computation.
 *
 * <p>Implements the ⭒ (star) operator and VRF nonce derivation as defined in
 * the Cardano ledger specification.</p>
 *
 * @see <a href="https://github.com/IntersectMBO/cardano-ledger/blob/master/libs/cardano-ledger-core/src/Cardano/Ledger/BaseTypes.hs">BaseTypes.hs</a>
 * @see <a href="https://github.com/IntersectMBO/ouroboros-consensus/blob/main/ouroboros-consensus-protocol/src/ouroboros-consensus-protocol/Ouroboros/Consensus/Protocol/Praos/VRF.hs">Praos VRF.hs</a>
 */
public class NonceUtil {

    private static final byte[] VRF_NONCE_PREFIX = new byte[]{(byte) 'N'};

    private NonceUtil() {
    }

    /**
     * ⭒ operator: combines two nonces.
     * <p>
     * {@code Nonce(a) ⭒ Nonce(b) = Blake2b_256(a || b)}
     * <p>
     * {@code null} represents NeutralNonce (identity element):
     * {@code x ⭒ null = x} and {@code null ⭒ x = x}.
     *
     * @param a first nonce bytes (or null for NeutralNonce)
     * @param b second nonce bytes (or null for NeutralNonce)
     * @return combined nonce, or null if both inputs are null
     */
    public static byte[] combineNonces(byte[] a, byte[] b) {
        if (a == null) return b;
        if (b == null) return a;

        byte[] combined = new byte[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return Blake2bUtil.blake2bHash256(combined);
    }

    /**
     * Derives the nonce contribution from a block's VRF output for <b>Praos</b> (Babbage+, era &ge; 6).
     * <p>
     * {@code eta = Blake2b_256(Blake2b_256("N" || vrfOutputBytes))}
     *
     * @param vrfOutputBytes the raw VRF output bytes
     * @return the derived nonce value
     * @see <a href="https://github.com/IntersectMBO/ouroboros-consensus/blob/main/ouroboros-consensus-protocol/src/ouroboros-consensus-protocol/Ouroboros/Consensus/Protocol/Praos/VRF.hs">Praos VRF.hs</a>
     */
    public static byte[] vrfNonceValue(byte[] vrfOutputBytes) {
        byte[] prefixed = new byte[VRF_NONCE_PREFIX.length + vrfOutputBytes.length];
        System.arraycopy(VRF_NONCE_PREFIX, 0, prefixed, 0, VRF_NONCE_PREFIX.length);
        System.arraycopy(vrfOutputBytes, 0, prefixed, VRF_NONCE_PREFIX.length, vrfOutputBytes.length);

        byte[] firstHash = Blake2bUtil.blake2bHash256(prefixed);
        return Blake2bUtil.blake2bHash256(firstHash);
    }

    /**
     * Derives the nonce contribution from a block's VRF output for <b>TPraos</b> (Shelley–Alonzo, era &le; 5).
     * <p>
     * {@code eta = Blake2b_256(vrfOutputBytes)}
     * <p>
     * Unlike Praos, TPraos uses a single hash with no prefix.
     *
     * @param vrfOutputBytes the raw VRF output bytes
     * @return the derived nonce value
     * @see <a href="https://github.com/IntersectMBO/ouroboros-consensus/blob/main/ouroboros-consensus-protocol/src/ouroboros-consensus-protocol/Ouroboros/Consensus/Protocol/TPraos.hs">TPraos.hs</a>
     */
    public static byte[] vrfNonceValueTPraos(byte[] vrfOutputBytes) {
        return Blake2bUtil.blake2bHash256(vrfOutputBytes);
    }

    /**
     * Converts a block's previous hash to a nonce value.
     * <p>
     * Direct wrap of block hash bytes (NO re-hashing).
     * {@code null} input returns {@code null} (NeutralNonce).
     *
     * @param prevHashHex the previous block hash as hex string, or null
     * @return the nonce bytes, or null if input is null
     */
    public static byte[] prevHashToNonce(String prevHashHex) {
        if (prevHashHex == null || prevHashHex.isEmpty()) {
            return null;
        }
        return HexUtil.decodeHexString(prevHashHex);
    }
}
