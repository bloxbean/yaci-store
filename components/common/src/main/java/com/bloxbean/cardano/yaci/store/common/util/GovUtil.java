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
     * //TODO: This is temporary implementation, it will be moved to CCL.
     * @param govActionIdBech32 The gov_action_id in bech32 format (e.g., "gov_action1...")
     * @return GovActionIdParts containing txHash and index
     * @throws IllegalArgumentException if the input is invalid
     */
    public static GovActionIdParts toGovActionId(String govActionIdBech32) {
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
            int index = decodedBytes[32] & 0xFF;

            return new GovActionIdParts(txHash, index);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse gov_action_id: " + e.getMessage(), e);
        }
    }

    /**
     * Record class to hold the result of gov_action_id conversion.
     */
    public record GovActionIdParts(String txHash, int index) {
        
        @Override
        public String toString() {
            return "GovActionIdParts{txHash='%s', index=%d}".formatted(txHash, index);
        }
    }
}
