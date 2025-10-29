package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

/**
 * Lightweight summary of transaction net transfers.
 *
 * Provides high-level overview without detailed per-address breakdowns.
 * Designed to reduce output size by ~90% compared to full TransactionNetTransfer.
 *
 * Use cases:
 * - Quick overview of activity in large time ranges
 * - Identifying transactions of interest before fetching full details
 * - Staying within Claude token limits for bulk analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionNetTransferSummary {

    /**
     * Transaction hash
     */
    private String txHash;

    /**
     * Slot number
     */
    private Long slot;

    /**
     * Block height
     */
    private Long blockHeight;

    /**
     * Transaction fee in lovelace
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger fee;

    /**
     * Total number of addresses involved in this transaction
     */
    private Integer totalAddresses;

    /**
     * Number of addresses that sent value (negative net)
     */
    private Integer senderCount;

    /**
     * Number of addresses that received value (positive net)
     */
    private Integer receiverCount;

    /**
     * Total ADA moved in the transaction (sum of absolute net changes / 2)
     * This represents the actual economic value transferred.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger totalAdaMoved;

    /**
     * Largest absolute net transfer amount in this transaction
     * Useful for identifying whale movements.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger largestNetTransfer;

    /**
     * Top senders by amount (up to 3)
     * Ordered by absolute net lovelace descending.
     */
    private List<AddressSummary> topSenders;

    /**
     * Top receivers by amount (up to 3)
     * Ordered by net lovelace descending.
     */
    private List<AddressSummary> topReceivers;

    /**
     * Whether this transaction involved native tokens
     * Note: Always false in current implementation (ADA-only analysis)
     */
    private Boolean hasTokens;

    /**
     * Whether this transaction involved smart contract interaction
     * Detected by presence of datum hashes, inline datums, or script references.
     */
    private Boolean hasScriptInteraction;

    /**
     * Simplified address summary showing only essential transfer information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AddressSummary {
        /**
         * Address
         */
        private String address;

        /**
         * Stake address if available
         */
        private String stakeAddress;

        /**
         * Net ADA change in lovelace
         * Positive = received, Negative = sent
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger netLovelace;
    }
}
