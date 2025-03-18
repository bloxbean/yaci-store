package com.bloxbean.cardano.yaci.store.api.epoch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EpochDto {
    private int epoch;
    private long startTime;
    private long endTime;
    private long firstBlockTime;
    private long lastBlockTime;
    private long blockCount;
    private long txCount;
    private String output;
    private String fees;
    private String activeStake;
}
