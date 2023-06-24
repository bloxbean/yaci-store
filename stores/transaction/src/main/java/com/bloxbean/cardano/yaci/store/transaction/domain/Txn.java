package com.bloxbean.cardano.yaci.store.transaction.domain;

import com.bloxbean.cardano.yaci.store.common.domain.TxOuput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Txn {
    private String txHash;
    private String blockHash;
    private Long blockNumber;
    private Long slot;
    private List<UtxoKey> inputs;
    private List<UtxoKey> outputs;
    private BigInteger fee;
    private Long ttl;
    private String auxiliaryDataHash;
    private Long validityIntervalStart;
    private String scriptDataHash;
    private List<UtxoKey> collateralInputs;
    private Set<String> requiredSigners;
    private Integer netowrkId;
    private UtxoKey collateralReturn;
    private TxOuput collateralReturnJson;
    private BigInteger totalCollateral;
    private List<UtxoKey> referenceInputs;
    private Boolean invalid;
}
