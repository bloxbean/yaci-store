package com.bloxbean.cardano.yaci.store.transaction.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.bloxbean.cardano.yaci.store.common.domain.TxOuput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Txn extends BlockAwareDomain {
    private String txHash;
    private String blockHash;
    private Long slot;
    private Integer txIndex;
    private Integer epoch;
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
    private BigInteger treasuryDonation;
    private Boolean invalid;
    
    /**
     * Raw CBOR bytes of the transaction body.
     * This field is only populated when Yaci is configured to return CBOR data.
     * See: YaciConfig.INSTANCE.setReturnTxBodyCbor(true)
     */
    private byte[] txBodyCbor;
}
