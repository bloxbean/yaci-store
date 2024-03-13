package com.bloxbean.cardano.yaci.store.script.domain;

import com.bloxbean.cardano.yaci.core.model.ExUnits;
import com.bloxbean.cardano.yaci.core.model.RedeemerTag;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Redeemer {
    private RedeemerTag tag;
    private Integer index;
    private String data;
    private ExUnits exUnits;
}

