package com.bloxbean.cardano.yaci.store.common.domain;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DrepVoteThresholds {
    private UnitInterval dvtMotionNoConfidence;
    private UnitInterval dvtCommitteeNormal;
    private UnitInterval dvtCommitteeNoConfidence;
    private UnitInterval dvtUpdateToConstitution;
    private UnitInterval dvtHardForkInitiation;
    private UnitInterval dvtPPNetworkGroup;
    private UnitInterval dvtPPEconomicGroup;
    private UnitInterval dvtPPTechnicalGroup;
    private UnitInterval dvtPPGovGroup;
    private UnitInterval dvtTreasuryWithdrawal;
}
