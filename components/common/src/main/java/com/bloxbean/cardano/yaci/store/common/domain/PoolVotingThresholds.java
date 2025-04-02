package com.bloxbean.cardano.yaci.store.common.domain;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
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
public class PoolVotingThresholds {
    private UnitInterval pvtMotionNoConfidence;
    private UnitInterval pvtCommitteeNormal;
    private UnitInterval pvtCommitteeNoConfidence;
    private UnitInterval pvtHardForkInitiation;
    private UnitInterval pvtPPSecurityGroup;
}
