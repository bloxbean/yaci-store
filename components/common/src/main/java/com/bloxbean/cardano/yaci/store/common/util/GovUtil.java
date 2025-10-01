package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Utility class for governance-related operations.
 * 
 * This class provides utilities for CIP-129 compatible governance identifiers
 * using Cardano Client Library's native support.
 */
@Slf4j
@UtilityClass
public class GovUtil {

    /**
     * Converts a CIP-129 compatible gov_action_id bech32 string to transaction hash and index.
     * 
     * @param govActionIdBech32 The gov_action_id in bech32 format (e.g., "gov_action1...")
     * @return GovActionIdParts containing txHash and index
     * @throws IllegalArgumentException if the input is invalid
     */
    public static GovActionIdParts convertGovActionIdToTxHashAndIndex(String govActionIdBech32) {
        if (govActionIdBech32 == null || govActionIdBech32.trim().isEmpty()) {
            throw new IllegalArgumentException("gov_action_id cannot be null or empty");
        }

        try {
            if (!govActionIdBech32.startsWith("gov_action1")) {
                throw new IllegalArgumentException("Invalid gov_action_id format. Expected 'gov_action1...' prefix");
            }

            byte[] decodedBytes = Bech32.decode(govActionIdBech32).data;
            
            if (decodedBytes.length != 33) {
                throw new IllegalArgumentException("Invalid gov_action_id: expected 33 bytes, got " + decodedBytes.length);
            }

            byte[] txHashBytes = Arrays.copyOfRange(decodedBytes, 0, 32);
            String txHash = HexUtil.encodeHexString(txHashBytes);
            long index = decodedBytes[32] & 0xFF;

            return new GovActionIdParts(txHash, index);

        } catch (Exception e) {
            log.error("Failed to convert gov_action_id to tx_hash and index: {}", govActionIdBech32, e);
            throw new IllegalArgumentException("Failed to parse gov_action_id: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a CIP-129 compatible gov_action_id from transaction hash and index.
     * This method uses CCL's native implementation.
     * 
     * @param txHash The transaction hash (32-byte hex string)
     * @param index The governance action index
     * @return CIP-129 compliant gov_action_id
     * @throws IllegalArgumentException if the input is invalid
     */
    public static String createGovActionId(String txHash, long index) {
        if (txHash == null || txHash.trim().isEmpty()) {
            throw new IllegalArgumentException("txHash cannot be null or empty");
        }
        
        if (index < 0 || index > 255) {
            throw new IllegalArgumentException("index must be between 0 and 255 (single byte)");
        }

        if (txHash.length() != 64) {
            throw new IllegalArgumentException("txHash must be 64 hex characters (32 bytes)");
        }

        try {
            return GovId.govAction(txHash, (int) index);
        } catch (Exception e) {
            log.error("Failed to create gov_action_id from txHash: {} and index: {}", txHash, index, e);
            throw new IllegalArgumentException("Failed to create gov_action_id: " + e.getMessage(), e);
        }
    }

    public static class GovActionIdParts {
        private final String txHash;
        private final long index;

        public GovActionIdParts(String txHash, long index) {
            this.txHash = txHash;
            this.index = index;
        }

        public String getTxHash() {
            return txHash;
        }

        public long getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return "GovActionIdParts{" +
                    "txHash='" + txHash + '\'' +
                    ", index=" + index +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GovActionIdParts that = (GovActionIdParts) o;
            return index == that.index && 
                   java.util.Objects.equals(txHash, that.txHash);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(txHash, index);
        }
    }
}
