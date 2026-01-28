package com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Blockfrost epoch stake distribution item.
 * Mirrors the API schema "epoch_stake_content".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BFEpochStakeDto {
    /** Stake address. */
    private String stakeAddress;
    /** Bech32 prefix of the pool delegated to. */
    private String poolId;
    /** Amount of active delegated stake in Lovelaces. */
    private String amount;
}
