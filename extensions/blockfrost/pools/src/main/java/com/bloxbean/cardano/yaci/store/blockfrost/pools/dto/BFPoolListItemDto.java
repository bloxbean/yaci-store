package com.bloxbean.cardano.yaci.store.blockfrost.pools.dto;

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
public class BFPoolListItemDto {
    private String poolId;
    private String hex;
    private String activeStake;
    private String liveStake;
    private Double liveSaturation;
    private Integer blocksMinted;
    private String declaredPledge;
    private Double marginCost;
    private String fixedCost;
    private BFPoolMetadataEmbedDto metadata;
}
