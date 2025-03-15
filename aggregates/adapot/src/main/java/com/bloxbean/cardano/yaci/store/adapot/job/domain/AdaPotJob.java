package com.bloxbean.cardano.yaci.store.adapot.job.domain;

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
public class AdaPotJob {
    private Integer epoch;
    private Long slot;
    private AdaPotJobType type;
    private AdaPotJobStatus status;
    private Long totalTime;
    private Long rewardCalcTime;
    private Long updateRewardTime;
    private Long stakeSnapshotTime;
    private Long drepDistrSnapshotTime;
    private String errorMessage;
}
