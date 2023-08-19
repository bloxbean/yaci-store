package com.bloxbean.cardano.yaci.store.transaction.domain;

import com.bloxbean.cardano.yaci.store.common.domain.TxOuput;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionDetails {
    private String hash;
    private Long blockHeight;
    private Long slot;
    private List<TxUtxo> inputs;
    private List<TxUtxo> outputs;
    private int utxoCount; //TransactionContent field
    private BigInteger totalOutput;
    private BigInteger fees;
    private Long ttl;
    private String auxiliaryDataHash;
    private Long validityIntervalStart;
    private String scriptDataHash;
    private List<TxUtxo> collateralInputs;
    private Set<String> requiredSigners;
    private Integer netowrkId;
    private TxOuput collateralReturn;
    private BigInteger totalCollateral;
    private List<TxUtxo> referenceInputs;
    private boolean invalid;
}
