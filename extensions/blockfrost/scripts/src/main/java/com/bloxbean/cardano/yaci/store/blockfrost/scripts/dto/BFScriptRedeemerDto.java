package com.bloxbean.cardano.yaci.store.blockfrost.scripts.dto;

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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFScriptRedeemerDto {
    private String txHash;
    private int txIndex;
    private String purpose;
    private String redeemerDataHash;
    private String datumHash;
    private String unitMem;
    private String unitSteps;
    private String fee;
}
