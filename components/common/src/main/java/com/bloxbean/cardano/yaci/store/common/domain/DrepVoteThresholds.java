package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DrepVoteThresholds {
    private BigDecimal dvtMotionNoConfidence;
    private BigDecimal dvtCommitteeNormal;
    private BigDecimal dvtCommitteeNoConfidence;
    private BigDecimal dvtUpdateToConstitution;
    private BigDecimal dvtHardForkInitiation;
    private BigDecimal dvtPPNetworkGroup;
    private BigDecimal dvtPPEconomicGroup;
    private BigDecimal dvtPPTechnicalGroup;
    private BigDecimal dvtPPGovGroup;
    private BigDecimal dvtTreasuryWithdrawal;
}
