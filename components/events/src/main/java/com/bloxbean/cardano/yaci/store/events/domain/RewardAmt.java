package com.bloxbean.cardano.yaci.store.events.domain;

import lombok.*;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RewardAmt {
    private RewardType rewardType;
    private String txHash; //null for pool rewards
    private String address;
    private BigInteger amount;
}
