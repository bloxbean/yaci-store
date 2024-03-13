package com.bloxbean.cardano.yaci.store.api.epochaggr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//This is a DTO class for epoch content to provide Blockfrost compatible response
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(
        ignoreUnknown = true
)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EpochContent {
    private Integer epoch;
    private long firstBlockTime;
    private long lastBlockTime;
    private long blockCount;
    private long txCount;
    private String output;
    private String fees;
}
