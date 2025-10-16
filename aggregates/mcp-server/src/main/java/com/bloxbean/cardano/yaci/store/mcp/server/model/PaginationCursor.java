package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Base64;

/**
 * Cursor for keyset-based pagination.
 *
 * Unlike OFFSET-based pagination (which scans all previous rows), keyset pagination
 * uses WHERE clauses to continue from the last seen record, providing O(log N) performance.
 *
 * The cursor is encoded as a Base64 string for stateless pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationCursor {

    /**
     * Last seen slot number
     */
    private Long lastSlot;

    /**
     * Last seen transaction hash
     */
    private String lastTxHash;

    /**
     * Last seen address (for queries that paginate at address level)
     */
    private String lastAddress;

    /**
     * Total number of records processed so far
     */
    private Integer totalProcessed;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Encode cursor to Base64 string for transmission to client.
     *
     * @return Base64 encoded cursor
     */
    public String encode() {
        try {
            String json = objectMapper.writeValueAsString(this);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to encode pagination cursor", e);
        }
    }

    /**
     * Decode cursor from Base64 string.
     *
     * @param encoded Base64 encoded cursor string
     * @return Decoded cursor object
     */
    public static PaginationCursor decode(String encoded) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encoded);
            String json = new String(decodedBytes);
            return objectMapper.readValue(json, PaginationCursor.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pagination cursor: " + e.getMessage(), e);
        }
    }
}
