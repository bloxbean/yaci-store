package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.client.governance.GovId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GovUtil class using CCL's GovId test vectors.
 */
class GovUtilTest {

    @Test
    @DisplayName("Should successfully convert CCL test vector 1")
    void shouldConvertCCLTestVector1() {
        String txHash = "0000000000000000000000000000000000000000000000000000000000000000";
        int index = 17;
        String govActionId = "gov_action1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqpzklpgpf";

        GovUtil.GovActionIdParts parts = GovUtil.convertGovActionIdToTxHashAndIndex(govActionId);

        assertEquals(txHash, parts.getTxHash());
        assertEquals(index, parts.getIndex());
    }

    @Test
    @DisplayName("Should successfully convert CCL test vector 2")
    void shouldConvertCCLTestVector2() {
        String txHash = "1111111111111111111111111111111111111111111111111111111111111111";
        int index = 0;
        String govActionId = "gov_action1zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygsq6dmejn";

        GovUtil.GovActionIdParts parts = GovUtil.convertGovActionIdToTxHashAndIndex(govActionId);

        assertEquals(txHash, parts.getTxHash());
        assertEquals(index, parts.getIndex());
    }

    @Test
    @DisplayName("Should handle round-trip conversion with CCL")
    void shouldHandleRoundTripConversionWithCCL() {
        String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        int index = 42;
        String govActionId = GovId.govAction(txHash, index);

        GovUtil.GovActionIdParts parts = GovUtil.convertGovActionIdToTxHashAndIndex(govActionId);

        assertEquals(txHash, parts.getTxHash());
        assertEquals(index, parts.getIndex());
    }

    @Test
    @DisplayName("Should throw exception for null input")
    void shouldThrowExceptionForNullInput() {
            assertThrows(IllegalArgumentException.class, () -> {
                GovUtil.convertGovActionIdToTxHashAndIndex(null);
            });
        }

    @Test
    @DisplayName("Should throw exception for empty input")
    void shouldThrowExceptionForEmptyInput() {
            assertThrows(IllegalArgumentException.class, () -> {
                GovUtil.convertGovActionIdToTxHashAndIndex("");
            });
        }


    @Test
    @DisplayName("Should throw exception for invalid prefix")
    void shouldThrowExceptionForInvalidPrefix() {
            assertThrows(IllegalArgumentException.class, () -> {
                GovUtil.convertGovActionIdToTxHashAndIndex("invalid_prefix1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqpzklpgpf");
            });
        }

    @Test
    @DisplayName("Should throw exception for malformed bech32 data")
    void shouldThrowExceptionForMalformedBech32Data() {
            assertThrows(IllegalArgumentException.class, () -> {
                GovUtil.convertGovActionIdToTxHashAndIndex("gov_action1invalid_characters");
            });
        }

    @Test
    @DisplayName("Should create gov_action_id using CCL")
    void shouldCreateGovActionIdUsingCCL() {
            String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            long index = 42L;

            String govActionId = GovUtil.createGovActionId(txHash, index);

            assertTrue(govActionId.startsWith("gov_action1"));
            GovUtil.GovActionIdParts parts = GovUtil.convertGovActionIdToTxHashAndIndex(govActionId);
            assertEquals(txHash, parts.getTxHash());
            assertEquals(index, parts.getIndex());
        }

    @Test
    @DisplayName("Should throw exception for invalid txHash")
    void shouldThrowExceptionForInvalidTxHash() {
            assertThrows(IllegalArgumentException.class, () -> {
                GovUtil.createGovActionId("invalid_txhash", 42L);
            });
        }

    @Test
    @DisplayName("Should throw exception for negative index")
    void shouldThrowExceptionForNegativeIndex() {
            assertThrows(IllegalArgumentException.class, () -> {
                GovUtil.createGovActionId("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", -1L);
            });
        }

    @Test
    @DisplayName("Should throw exception for index too large")
    void shouldThrowExceptionForIndexTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> {
            GovUtil.createGovActionId("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890", 256L);
        });
    }
    @Test
    @DisplayName("Should create GovActionIdParts with correct values")
    void shouldCreateGovActionIdPartsWithCorrectValues() {
            String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            long index = 42L;

            GovUtil.GovActionIdParts parts = new GovUtil.GovActionIdParts(txHash, index);

            assertEquals(txHash, parts.getTxHash());
            assertEquals(index, parts.getIndex());
        }

    @Test
    @DisplayName("Should have correct toString representation")
    void shouldHaveCorrectToStringRepresentation() {
            String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            long index = 42L;

            GovUtil.GovActionIdParts parts = new GovUtil.GovActionIdParts(txHash, index);
            String toString = parts.toString();

            assertTrue(toString.contains("GovActionIdParts"));
            assertTrue(toString.contains("txHash='" + txHash + "'"));
            assertTrue(toString.contains("index=" + index));
        }

    @Test
    @DisplayName("Should be equal for same txHash and index")
    void shouldBeEqualForSameTxHashAndIndex() {
            String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            long index = 42L;

            GovUtil.GovActionIdParts parts1 = new GovUtil.GovActionIdParts(txHash, index);
            GovUtil.GovActionIdParts parts2 = new GovUtil.GovActionIdParts(txHash, index);

            assertEquals(parts1, parts2);
            assertEquals(parts1.hashCode(), parts2.hashCode());
        }

    @Test
    @DisplayName("Should not be equal for different txHash")
    void shouldNotBeEqualForDifferentTxHash() {
            String txHash1 = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            String txHash2 = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321";
            long index = 42L;

            GovUtil.GovActionIdParts parts1 = new GovUtil.GovActionIdParts(txHash1, index);
            GovUtil.GovActionIdParts parts2 = new GovUtil.GovActionIdParts(txHash2, index);

            assertNotEquals(parts1, parts2);
        }

    @Test
    @DisplayName("Should not be equal for different index")
    void shouldNotBeEqualForDifferentIndex() {
            String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            long index1 = 42L;
            long index2 = 43L;

            GovUtil.GovActionIdParts parts1 = new GovUtil.GovActionIdParts(txHash, index1);
            GovUtil.GovActionIdParts parts2 = new GovUtil.GovActionIdParts(txHash, index2);

            assertNotEquals(parts1, parts2);
        }

    @Test
    @DisplayName("Should handle zero index")
    void shouldHandleZeroIndex() {
            String txHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
            long zeroIndex = 0L;

            GovUtil.GovActionIdParts parts = new GovUtil.GovActionIdParts(txHash, zeroIndex);

            assertEquals(txHash, parts.getTxHash());
            assertEquals(zeroIndex, parts.getIndex());
        }

    @Test
    @DisplayName("Should handle maximum single-byte index value")
    void shouldHandleMaximumSingleByteIndexValue() {
        String txHash = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        int maxIndex = 255;
        
        String govActionId = GovId.govAction(txHash, maxIndex);
        GovUtil.GovActionIdParts parts = GovUtil.convertGovActionIdToTxHashAndIndex(govActionId);

        assertEquals(txHash, parts.getTxHash());
        assertEquals(maxIndex, parts.getIndex());
    }
}
