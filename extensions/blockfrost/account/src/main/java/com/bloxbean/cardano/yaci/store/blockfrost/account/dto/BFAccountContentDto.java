package com.bloxbean.cardano.yaci.store.blockfrost.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFAccountContentDto {
    private String stakeAddress;
    private boolean active;
    private boolean registered;
    private Integer activeEpoch;
    private String controlledAmount;
    private String rewardsSum;
    private String withdrawalsSum;
    private String reservesSum;
    private String treasurySum;
    private String withdrawableAmount;
    private String poolId;
    private String drepId;
}
