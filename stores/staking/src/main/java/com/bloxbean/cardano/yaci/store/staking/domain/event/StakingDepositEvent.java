package com.bloxbean.cardano.yaci.store.staking.domain.event;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StakingDepositEvent {
    private EventMetadata metadata;
    private int stakeKeyRegistrationCount;
    private int stakeKeyDeRegistrationCount;
    private int stakePoolRegistrationCount;
}
