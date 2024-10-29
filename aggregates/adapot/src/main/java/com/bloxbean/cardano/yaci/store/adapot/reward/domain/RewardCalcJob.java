package com.bloxbean.cardano.yaci.store.adapot.reward.domain;

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
public class RewardCalcJob {
    private Integer epoch;
    private Long slot;
    private RewardCalcStatus status;
    private Long totalTime;
    private Long rewardCalcTime;
    private Long updateRewardTime;
    private Long stakeSnapshotTime;
    private String errorMessage;
}
