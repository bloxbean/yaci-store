package com.bloxbean.cardano.yaci.store.transaction.util;

/**
 * Ledger size of a transaction, as stored in {@code transaction_cbor.cbor_size}.
 * <p>
 * The ledger (and db-sync {@code tx.size}) measures {@code [body, witnesses, auxiliary_data]}
 * and never includes the Alonzo+ {@code is_valid} flag. The full transaction CBOR Yaci assembles
 * for {@code store.transaction.save-full-tx-cbor} is the wire form:
 * <ul>
 *     <li>Shelley–Mary: {@code 0x83 + body + witnesses + aux|null} (same as the ledger size)</li>
 *     <li>Alonzo onwards: {@code 0x84 + body + witnesses + is_valid + aux|null} (one byte more)</li>
 * </ul>
 * Any other array header is returned as its length so that a future era does not drop the size.
 * A body-only CBOR (a map) is not a full transaction, so its size is unknown.
 */
public final class TxSizeUtil {

    private static final int ARRAY_3 = 0x83;
    private static final int ARRAY_4 = 0x84;
    private static final int MAJOR_TYPE_MASK = 0xe0;
    private static final int MAJOR_TYPE_ARRAY = 0x80;

    private TxSizeUtil() {
    }

    /**
     * @param txCbor full transaction CBOR
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
        if (header == ARRAY_3 || (header & MAJOR_TYPE_MASK) == MAJOR_TYPE_ARRAY) {
            return txCbor.length;
        }
        return null;
    }
}
