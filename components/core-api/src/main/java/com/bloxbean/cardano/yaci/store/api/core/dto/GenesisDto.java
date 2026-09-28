package com.bloxbean.cardano.yaci.store.api.core.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

/**
 * Genesis information in Blockfrost compatible <code>/genesis</code> shape.
 */
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GenesisDto(
        double activeSlotsCoefficient,
        int updateQuorum,
        String maxLovelaceSupply,
        long networkMagic,
        long epochLength,
        long systemStart,
        long slotsPerKesPeriod,
        long slotLength,
        int maxKesEvolutions,
        int securityParam
) {
}
