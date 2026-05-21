package com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class BFTxInputDto {
    private String address;
    private List<BFAmountDto> amount;
    private String txHash;
    private Integer outputIndex;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
    private Boolean collateral;
    private Boolean reference;
}
