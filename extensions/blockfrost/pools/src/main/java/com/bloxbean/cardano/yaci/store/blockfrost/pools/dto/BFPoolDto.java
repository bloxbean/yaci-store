package com.bloxbean.cardano.yaci.store.blockfrost.pools.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFPoolDto {
    private String poolId;
    private String hex;
    private String vrfKey;
    private Integer blocksMinted;
    private Integer blocksEpoch;
    private String liveStake;
    private Double liveSize;
    private Double liveSaturation;
    private Integer liveDelegators;
    private String activeStake;
    private Double activeSize;
    private String declaredPledge;
    private String livePledge;
    private Double marginCost;
    private String fixedCost;
    private String rewardAccount;
    private List<String> owners;
    private List<String> registration;
    private List<String> retirement;
    private Object calidusKey;
}
