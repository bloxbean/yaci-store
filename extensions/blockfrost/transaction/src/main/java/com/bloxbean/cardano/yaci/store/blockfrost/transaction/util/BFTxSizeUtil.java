package com.bloxbean.cardano.yaci.store.blockfrost.transaction.util;

/**
 * Derives the Blockfrost {@code size} of a transaction from its stored CBOR.
 * <p>
 * Blockfrost (db-sync {@code tx.size}) reports the ledger size, which the ledger computes
 * as {@code [body, witnesses, auxiliary_data]} and never includes the Alonzo+ {@code is_valid} flag.
 * The full transaction CBOR stored with {@code store.transaction.save-full-tx-cbor} is the wire form:
 * <ul>
 *     <li>Shelley–Mary: {@code 0x83 + body + witnesses + aux|null} (same as the ledger size)</li>
 *     <li>Alonzo onwards: {@code 0x84 + body + witnesses + is_valid + aux|null} (one byte more)</li>
 * </ul>
 * With {@code store.transaction.save-cbor} only the transaction body (a CBOR map) is stored,
 * so the size cannot be derived and {@code null} is returned instead of the body length.
 */
public final class BFTxSizeUtil {

    private static final int ARRAY_3 = 0x83;
    private static final int ARRAY_4 = 0x84;

    private BFTxSizeUtil() {
    }

    /**
     * @param txCbor stored transaction CBOR
     * @return ledger size in bytes, or null when {@code txCbor} is not a full transaction
     */
    public static Integer ledgerSize(byte[] txCbor) {
        if (txCbor == null || txCbor.length == 0) {
            return null;
        }

        int header = txCbor[0] & 0xff;
        if (header == ARRAY_4) {
            // Ledger encodes the same segments under a 3-element header (also one byte) without is_valid
            return txCbor.length - 1;
        }
        if (header == ARRAY_3) {
            return txCbor.length;
        }
        return null;
    }
}
