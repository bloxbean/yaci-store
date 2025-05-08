package com.bloxbean.cardano.yaci.store.api.governanceaggr.dto;

import com.bloxbean.cardano.client.transaction.spec.governance.DRepType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
// Auto-Abstain and No-Confidence DRep
public class SpecialDRepDto {
    private DRepType dRepType;
    private BigInteger votingPower;
}
