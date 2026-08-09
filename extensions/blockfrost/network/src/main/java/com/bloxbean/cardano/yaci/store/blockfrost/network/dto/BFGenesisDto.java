package com.bloxbean.cardano.yaci.store.blockfrost.network.dto;

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
public class BFGenesisDto {

    private Double activeSlotsCoefficient;
    private Integer updateQuorum;
    private String maxLovelaceSupply;
    private Long networkMagic;
    private Long epochLength;
    private Long systemStart;
    private Long slotsPerKesPeriod;
    private Integer slotLength;
    private Integer maxKesEvolutions;
    private Integer securityParam;
}
