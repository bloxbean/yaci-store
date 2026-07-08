package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AddressUtxo extends BlockAwareDomain implements Serializable {
    private String txHash;
    private Integer outputIndex;
    /** Index of the producing transaction within its block (0-based). Populated by the
     *  utxo processors so downstream consumers can order same-slot updates deterministically. */
    private Integer txIndex;
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
    private Boolean isCollateralReturn;
}
