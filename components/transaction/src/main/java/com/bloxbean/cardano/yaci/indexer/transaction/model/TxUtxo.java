package com.bloxbean.cardano.yaci.indexer.transaction.model;

import com.bloxbean.carano.yaci.indexer.common.model.Amt;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TxUtxo {
    private String txHash;
    private Integer outputIndex;
    private String ownerAddr;
    private String ownerStakeAddr;
    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;

    private JsonNode inlineDatumJson;
}
