package com.bloxbean.cardano.yaci.store.api.script.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TxRedeemerDto {
    private Integer txIndex;
    private String purpose;
    private String scriptHash;
    private String datumHash;
    private String redeemerDataHash;
    private String unitMem;
    private String unitSteps;
    //private String fee; //TODO -- fee consumed to run the script
}
