package com.bloxbean.cardano.yaci.store.events.domain;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RewardEvent {
    private EventMetadata metadata;
    private List<RewardAmt> rewards;
}
