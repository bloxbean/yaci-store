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
public class BFAccountDelegationDto {
    private int activeEpoch;
    private String txHash;
    private String amount;
    private String poolId;
    private Long txSlot;
    private Long blockTime;
    private Long blockHeight;
}
