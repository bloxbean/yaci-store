package com.bloxbean.cardano.yaci.store.events.domain;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RewardRestEvent {
    private int earnedEpoch;
    private int spendableEpoch;
    private long slot;
    private List<RewardRestAmt> rewards;
}
