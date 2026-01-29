package com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Blockfrost epoch content response.
 * Mirrors the API schema "epoch_content".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFEpochDto {
    /** Epoch number. */
    private Integer epoch;
    /** Unix time of the start of the epoch. */
    private Long startTime;
    /** Unix time of the end of the epoch. */
    private Long endTime;
    /** Unix time of the first block of the epoch. */
    private Long firstBlockTime;
    /** Unix time of the last block of the epoch. */
    private Long lastBlockTime;
    /** Number of blocks within the epoch. */
    private Integer blockCount;
    /** Number of transactions within the epoch. */
    private Long txCount;
    /** Sum of all the transactions within the epoch in Lovelaces. */
    private String output;
    /** Sum of all the fees within the epoch in Lovelaces. */
    private String fees;
    /** Sum of all the active stakes within the epoch in Lovelaces. */
    private String activeStake;
}
