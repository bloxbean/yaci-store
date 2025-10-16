package com.bloxbean.cardano.yaci.store.mcp.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * DTO for batch net transfer query results.
 * Represents net ADA transfer for a single address in a single transaction.
 * Multiple rows per transaction (one per address involved).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionNetTransferDto {

    /**
     * Transaction hash
     */
    private String txHash;

    /**
     * Slot number
     */
    private Long slot;

    /**
     * Epoch number
     */
    private Integer epoch;

    /**
     * Block number
     */
    private Long block;

    /**
     * Transaction fee in lovelace
     */
    private BigInteger fee;

    /**
     * Address involved in this transaction
     */
    private String address;

    /**
     * Stake address associated with this address
     */
    private String stakeAddress;

    /**
     * Net lovelace change for this address
     * Positive = received, Negative = sent
     */
    private BigInteger netLovelace;

    /**
     * Total lovelace in inputs from this address
     */
    private BigInteger inputLovelace;

    /**
     * Total lovelace in outputs to this address
     */
    private BigInteger outputLovelace;

    /**
     * Whether this address is a net sender (lost ADA)
     */
    private Boolean isSender;

    /**
     * Whether this address is a net receiver (gained ADA)
     */
    private Boolean isReceiver;
}
