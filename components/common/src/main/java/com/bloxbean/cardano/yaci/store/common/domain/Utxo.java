package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.math.BigInteger;
import java.util.List;

/**
 * This class is used to represent UTXO for controller API
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Utxo {

    private String txHash;
    private int outputIndex;
    private String address;
    private List<Amount> amount;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
    private Integer epoch;
    private Long blockNumber;
    private Long blockTime;
    //Block hash, to align with Blockfrost
    private String block;

    //Kept for backward compatibility with callers created before the block field was added
    public Utxo(String txHash, int outputIndex, String address, List<Amount> amount, String dataHash,
                String inlineDatum, String referenceScriptHash, Integer epoch, Long blockNumber, Long blockTime) {
        this(txHash, outputIndex, address, amount, dataHash, inlineDatum, referenceScriptHash, epoch,
                blockNumber, blockTime, null);
    }

    //To align with Blockfrost. tx_index is the deprecated alias of output_index for address utxos
    @JsonProperty("tx_index")
    public int getTxIndex() {
        return outputIndex;
    }

    @Getter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Amount {
        private String unit;
        @JsonSerialize(using = ToStringSerializer.class)
        private BigInteger quantity;
    }
}
