package com.bloxbean.cardano.yaci.store.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressUtxo {
    private String txHash;
    private Integer outputIndex;
    private Long slot;
    private Long block;
    private String blockHash;
    private String ownerAddr;
    private String ownerStakeAddr;
    private String ownerPaymentCredential;
    private String ownerStakeCredential;
    private BigInteger lovelaceAmount;
    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String scriptRef;
    private Boolean spent;
    private Long spentAtSlot;
    private String spentTxHash;
    private Boolean isCollateralReturn;
}
