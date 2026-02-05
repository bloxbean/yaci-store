package com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Blockfrost epoch stake distribution item filtered by pool.
 * Mirrors the API schema "epoch_stake_pool_content".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFEpochStakePoolDto {
    /** Stake address. */
    private String stakeAddress;
    /** Amount of active delegated stake in Lovelaces. */
    private String amount;
}
