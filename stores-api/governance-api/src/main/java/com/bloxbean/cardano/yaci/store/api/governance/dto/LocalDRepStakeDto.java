package com.bloxbean.cardano.yaci.store.api.governance.dto;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LocalDRepStakeDto {
    private String drepHash;

    private DrepType drepType;

    private BigInteger amount;

    private Integer epoch;
}
