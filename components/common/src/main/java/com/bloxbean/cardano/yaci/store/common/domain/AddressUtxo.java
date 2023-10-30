package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AddressUtxo extends BlockAwareDomain {
    private String txHash;
    private Integer outputIndex;
    private Long slot;
    private String blockHash;
    private Integer epoch;
    private String ownerAddr;
    private String ownerStakeAddr;
    private String ownerPaymentCredential;
    private String ownerStakeCredential;
    private BigInteger lovelaceAmount;
    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String scriptRef;
    private String referenceScriptHash;
    private Boolean spent;
    private Long spentAtSlot;
    private Long spentAtBlock;
    private String spentAtBlockHash;
    private Long spentBlockTime;
    private Integer spentEpoch;
    private String spentTxHash;
    private Boolean isCollateralReturn;
}
