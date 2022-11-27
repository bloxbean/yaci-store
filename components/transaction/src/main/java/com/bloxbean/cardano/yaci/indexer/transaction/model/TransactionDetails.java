package com.bloxbean.cardano.yaci.indexer.transaction.model;

import com.bloxbean.carano.yaci.indexer.common.model.TxOuput;
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
    private String txHash;
    private long blockNumber;
    private long slot;
    private List<TxUtxo> inputs;
    private List<TxUtxo> outputs;
    private BigInteger fee;
    private long ttl;
    private String auxiliaryDataHash;
    private long validityIntervalStart;
    private String scriptDataHash;
    private List<TxUtxo> collateralInputs;
    private Set<String> requiredSigners;
    private int netowrkId;
    private TxOuput collateralReturn;
    private BigInteger totalCollateral;
    private List<TxUtxo> referenceInputs;
    private boolean invalid;
}
