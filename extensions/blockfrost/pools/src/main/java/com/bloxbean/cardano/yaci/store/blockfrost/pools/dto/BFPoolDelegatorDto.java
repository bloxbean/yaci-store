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
public class BFPoolDelegatorDto {
    /** Bech32 stake address of the delegator. Always non-null. */
    private String address;
    /** Current delegated stake in lovelace. Null when adapot aggregate is disabled. */
    private String liveStake;
}
