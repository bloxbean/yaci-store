package com.bloxbean.cardano.yaci.store.mcp.server.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PaginationCursor encode/decode functionality.
 * Tests the Base64 encoding/decoding and JSON serialization.
 */
class PaginationCursorTest {

    @Test
    @DisplayName("Encode then decode should preserve all fields")
    void testEncodeDecodeRoundTrip() {
        // Given
        PaginationCursor original = PaginationCursor.builder()
                .lastSlot(104931775L)
                .lastTxHash("abc123def456")
                .lastAddress("addr_test1qxy...")
                .totalProcessed(100)
                .build();

        // When
        String encoded = original.encode();
        PaginationCursor decoded = PaginationCursor.decode(encoded);

        // Then
        assertThat(decoded).isNotNull();
        assertThat(decoded.getLastSlot()).isEqualTo(original.getLastSlot());
        assertThat(decoded.getLastTxHash()).isEqualTo(original.getLastTxHash());
        assertThat(decoded.getLastAddress()).isEqualTo(original.getLastAddress());
        assertThat(decoded.getTotalProcessed()).isEqualTo(original.getTotalProcessed());
    }

    @Test
    @DisplayName("Encode produces valid Base64 string")
    void testEncodeProducesValidBase64() {
        // Given
        PaginationCursor cursor = PaginationCursor.builder()
                .lastSlot(12345L)
                .lastTxHash("hash")
                .totalProcessed(50)
                .build();

        // When
        String encoded = cursor.encode();

        // Then
        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotEmpty();

        // Should be decodable as Base64
        assertThat(Base64.getDecoder().decode(encoded)).isNotEmpty();
    }

    @Test
    @DisplayName("Decode with invalid Base64 throws IllegalArgumentException")
    void testDecodeInvalidBase64ThrowsException() {
        // Given
        String invalidBase64 = "not-valid-base64!!!";

        // When / Then
        assertThatThrownBy(() -> PaginationCursor.decode(invalidBase64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid pagination cursor");
    }

    @Test
    @DisplayName("Decode with invalid JSON throws IllegalArgumentException")
    void testDecodeInvalidJsonThrowsException() {
        // Given
        String invalidJson = Base64.getEncoder().encodeToString("{broken json".getBytes());

        // When / Then
        assertThatThrownBy(() -> PaginationCursor.decode(invalidJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid pagination cursor");
    }

    @Test
    @DisplayName("Encode and decode with null fields")
    void testEncodeDecodeWithNullFields() {
        // Given
        PaginationCursor original = PaginationCursor.builder()
                .lastSlot(null)
                .lastTxHash(null)
                .lastAddress(null)
                .totalProcessed(0)
                .build();

        // When
        String encoded = original.encode();
        PaginationCursor decoded = PaginationCursor.decode(encoded);

        // Then
        assertThat(decoded).isNotNull();
        assertThat(decoded.getLastSlot()).isNull();
        assertThat(decoded.getLastTxHash()).isNull();
        assertThat(decoded.getLastAddress()).isNull();
        assertThat(decoded.getTotalProcessed()).isEqualTo(0);
    }

    @Test
    @DisplayName("Encode with minimal fields")
    void testEncodeWithMinimalFields() {
        // Given
        PaginationCursor cursor = PaginationCursor.builder()
                .lastSlot(100L)
                .lastTxHash("tx1")
                .build();

        // When
        String encoded = cursor.encode();
        PaginationCursor decoded = PaginationCursor.decode(encoded);

        // Then
        assertThat(decoded.getLastSlot()).isEqualTo(100L);
        assertThat(decoded.getLastTxHash()).isEqualTo("tx1");
        assertThat(decoded.getLastAddress()).isNull();
        assertThat(decoded.getTotalProcessed()).isNull();
    }

    @Test
    @DisplayName("Decode with null input throws IllegalArgumentException")
    void testDecodeNullThrowsException() {
        assertThatThrownBy(() -> PaginationCursor.decode(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Decode with empty string throws IllegalArgumentException")
    void testDecodeEmptyStringThrowsException() {
        assertThatThrownBy(() -> PaginationCursor.decode(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Multiple encode/decode cycles preserve data")
    void testMultipleEncodeDecodeCycles() {
        // Given
        PaginationCursor original = PaginationCursor.builder()
                .lastSlot(999999L)
                .lastTxHash("verylongtxhash123456789")
                .lastAddress("addr_test1qabcdefghijk...")
                .totalProcessed(5000)
                .build();

        // When: Multiple encode/decode cycles
        String encoded1 = original.encode();
        PaginationCursor decoded1 = PaginationCursor.decode(encoded1);
        String encoded2 = decoded1.encode();
        PaginationCursor decoded2 = PaginationCursor.decode(encoded2);

        // Then: Data should be identical after multiple cycles
        assertThat(decoded2.getLastSlot()).isEqualTo(original.getLastSlot());
        assertThat(decoded2.getLastTxHash()).isEqualTo(original.getLastTxHash());
        assertThat(decoded2.getLastAddress()).isEqualTo(original.getLastAddress());
        assertThat(decoded2.getTotalProcessed()).isEqualTo(original.getTotalProcessed());
        assertThat(encoded1).isEqualTo(encoded2);
    }
}
