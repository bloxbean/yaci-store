package com.bloxbean.cardano.yaci.store.utxo.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
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
public class AddressUtxoEntity extends BlockAwareEntity {

    private String txHash;

    private Integer outputIndex;

    private Long slot;

    private String blockHash;

    private Integer epoch;

    private String ownerAddr;

    //Only set if address doesn't fit in ownerAddr field. Required for few Byron Era addr
    private String ownerAddrFull;

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
