package com.bloxbean.cardano.yaci.store.epoch.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EpochParam extends BlockAwareDomain {
    private Integer epoch;
    private ProtocolParams params;
    private Long slot;
}
