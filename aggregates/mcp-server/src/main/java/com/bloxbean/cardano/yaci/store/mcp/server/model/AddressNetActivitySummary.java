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
 * Aggregated net activity for a single address across multiple transactions.
 *
 * This provides a high-level view of an address's activity in a time period,
 * aggregating net transfers across all transactions the address participated in.
 *
 * Perfect for:
 * - Whale watching (finding large movers)
 * - Identifying most active addresses
 * - Daily/weekly summaries
 * - Address behavior analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AddressNetActivitySummary {

    /**
     * Address
     */
    private String address;

    /**
     * Stake address if available
     */
    private String stakeAddress;

    /**
     * Number of transactions this address participated in during the time range
     */
    private Integer transactionCount;

    /**
     * Total net ADA change across all transactions (positive = net receiver, negative = net sender)
     * This is the sum of all net transfers for this address.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger totalNetLovelace;

    /**
     * Total ADA sent (sum of all negative net transfers)
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger totalSent;

    /**
     * Total ADA received (sum of all positive net transfers)
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger totalReceived;

    /**
     * List of transaction hashes this address was involved in
     * Can be used to drill down into specific transactions
     */
    private List<String> involvedTransactions;

    /**
     * First activity slot in the time range
     */
    private Long firstSlot;

    /**
     * Last activity slot in the time range
     */
    private Long lastSlot;

    /**
     * Classification based on activity pattern
     */
    private AddressClassification classification;

    /**
     * Address classification based on net activity
     */
    public enum AddressClassification {
        /**
         * Address sent more than it received (totalNetLovelace &lt; 0)
         */
        NET_SENDER,

        /**
         * Address received more than it sent (totalNetLovelace > 0)
         */
        NET_RECEIVER,

        /**
         * Address sent and received roughly equal amounts (within 1% of total volume)
         */
        BALANCED,

        /**
         * Address was involved in many transactions (>10)
         */
        HIGH_FREQUENCY
    }
}
