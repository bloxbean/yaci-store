package com.bloxbean.cardano.yaci.store.blockfrost.governance.dto;

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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BFDRepDto {
    private String drepId;
    private String hex;
    private String amount;
    private Boolean active;
    private Integer activeEpoch;
    private Boolean hasScript;
    private Boolean retired;
    private Boolean expired;
    private Integer lastActiveEpoch;
}
