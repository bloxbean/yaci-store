package com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFTxRedeemerDto {
    private Integer txIndex;
    private String purpose;
    private String scriptHash;
    private String redeemerDataHash;
    private String datumHash; // deprecated, same value as redeemer_data_hash
    private String unitMem;
    private String unitSteps;
    private String fee;
}
