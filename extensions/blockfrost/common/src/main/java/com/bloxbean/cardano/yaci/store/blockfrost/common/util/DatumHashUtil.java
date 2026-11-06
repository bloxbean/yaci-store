package com.bloxbean.cardano.yaci.store.blockfrost.common.util;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DatumHashUtil {

    private DatumHashUtil() {
    }

    /**
     * Returns the datum hash of a UTxO, deriving it from the inline datum when no hash is stored.
     * <p>
     * {@code address_utxo.data_hash} is only populated for hash-referenced datums. For inline datums the ledger
     * defines the hash as blake2b-256 over the datum's original CBOR bytes, so the stored bytes are hashed as-is
     * (never re-encoded, since CBOR serialization is not unique).
     */
    public static String resolveDataHash(String dataHash, String inlineDatumHex) {
        if (dataHash != null && !dataHash.isBlank())
            return dataHash;
        if (inlineDatumHex == null || inlineDatumHex.isBlank())
            return dataHash;

        try {
            return HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(HexUtil.decodeHexString(inlineDatumHex)));
        } catch (Exception e) {
            log.warn("Unable to compute datum hash from inline datum", e);
            return dataHash;
        }
    }
}
