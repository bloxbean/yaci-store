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
import java.util.Map;

/**
 * Represents the net transfer analysis for a transaction.
 * Shows how much ADA and tokens each address gained or lost.
 *
 * This solves the UTXO chain problem where most amounts appear in both inputs and outputs.
 * By calculating net changes per address, we can identify actual value movement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionNetTransfer {

    /**
     * Transaction hash this analysis is for
     */
    private String txHash;

    /**
     * Block height
     */
    private Long blockHeight;

    /**
     * Slot number
     */
    private Long slot;

    /**
     * Total transaction fee paid (always a cost to the sender)
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger fee;

    /**
     * Net transfer per address.
     * Key: address
     * Value: NetTransferPerAddress showing gains/losses
     */
    private Map<String, NetTransferPerAddress> netTransfers;

    /**
     * List of addresses that lost value (senders)
     * These are addresses with negative net ADA after accounting for fees
     */
    private List<String> senders;

    /**
     * List of addresses that gained value (receivers)
     * These are addresses with positive net ADA
     */
    private List<String> receivers;

    /**
     * Summary statistics
     */
    private TransferSummary summary;

    /**
     * Represents net transfer for a single address in the transaction
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class NetTransferPerAddress {
        /**
         * Address
         */
        private String address;

        /**
         * Stake address if available
         */
        private String stakeAddress;

        /**
         * Net ADA change in lovelace (positive = received, negative = sent)
         * This is: (sum of outputs) - (sum of inputs) for this address
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger netLovelace;

        /**
         * Net change for each native asset
         * Key: unit (policyId + assetName)
         * Value: net quantity (positive = received, negative = sent)
         */
        private Map<String, AssetNetChange> netAssets;

        /**
         * Whether this address is a net sender (lost value)
         */
        private boolean isSender;

        /**
         * Whether this address is a net receiver (gained value)
         */
        private boolean isReceiver;

        /**
         * Total value in inputs from this address
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger totalInputs;

        /**
         * Total value in outputs to this address
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger totalOutputs;
    }

    /**
     * Represents net change for a specific asset
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AssetNetChange {
        /**
         * Asset unit (policyId + assetName in hex)
         */
        private String unit;

        /**
         * Policy ID
         */
        private String policyId;

        /**
         * Asset name (hex)
         */
        private String assetName;

        /**
         * Net quantity change (positive = received, negative = sent)
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger netQuantity;

        /**
         * Total quantity in inputs
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger totalInputQuantity;

        /**
         * Total quantity in outputs
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger totalOutputQuantity;
    }

    /**
     * Summary statistics for the transaction
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TransferSummary {
        /**
         * Total number of addresses involved
         */
        private int totalAddresses;

        /**
         * Number of sender addresses
         */
        private int senderCount;

        /**
         * Number of receiver addresses
         */
        private int receiverCount;

        /**
         * Number of different assets transferred (excluding lovelace)
         */
        private int assetTypesCount;

        /**
         * Total ADA moved (sum of absolute values of net changes, divided by 2)
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger totalAdaMoved;

        /**
         * Whether this is a simple transfer (1 sender, 1 receiver, no change address reuse)
         */
        private boolean isSimpleTransfer;

        /**
         * Whether this involves smart contracts
         */
        private boolean hasScriptInteraction;
    }
}
