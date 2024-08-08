package com.bloxbean.cardano.yaci.store.governance.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LocalHardForkInitiation {
    private String govActionTxHash;

    private int govActionIndex;

    private Integer majorVersion;

    private Integer minorVersion;

    private int epoch;

    private long slot;
}
