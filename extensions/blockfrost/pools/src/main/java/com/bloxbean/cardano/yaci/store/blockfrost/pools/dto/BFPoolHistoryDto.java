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
public class BFPoolHistoryDto {
    /** Epoch number. Always non-null. */
    private Integer epoch;
    /** Blocks produced by pool this epoch. Always non-null. */
    private Integer blocks;
    /** Pool operator rewards this epoch in lovelace (leader reward type). Always non-null. */
    private String fees;
    /** Total stake delegated to pool this epoch in lovelace. Null when adapot aggregate is disabled. */
    private String activeStake;
    /** Pool's share of total active stake (0.0–1.0). Null when adapot aggregate is disabled. */
    private Double activeSize;
    /** Number of delegators this epoch. Null when adapot aggregate is disabled. */
    private Integer delegatorsCount;
    /** Total rewards distributed this epoch in lovelace (all reward types). Null when adapot aggregate is disabled. */
    private String rewards;
}
